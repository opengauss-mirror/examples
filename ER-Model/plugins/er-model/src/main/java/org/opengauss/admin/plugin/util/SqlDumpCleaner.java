/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 *
 * openGauss is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *
 * http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITFOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package org.opengauss.admin.plugin.util;

import org.opengauss.admin.plugin.enums.DatabaseDialect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Clean SQL dumps by stripping data statements and retaining only DDL.
 * - Remove PostgreSQL/openGauss COPY … FROM stdin data blocks
 * - Split statements with awareness of quotes, comments, dollar-quoted strings, and backticks
 * - Whitelist: CREATE TABLE / ALTER TABLE / CREATE INDEX
 *
 * @since 2025-01-07
 */
public final class SqlDumpCleaner {
    private SqlDumpCleaner() {}

    /**
     * Remove data statements from a SQL dump, keeping only DDL statements supported by the ER modeler.
     *
     * @param sql     raw SQL dump text
     * @param dialect target database dialect (currently informational)
     * @return cleaned SQL containing only whitelisted DDL statements
     */
    public static String keepDdlOnly(String sql, DatabaseDialect dialect) {
        if (sql == null || sql.isBlank()) {
            return "";
        }
        String noCopy = stripPgCopyData(sql);
        List<String> statements = splitSqlStatements(noCopy);

        StringBuilder ddlOnly = new StringBuilder(noCopy.length());
        for (String statement : statements) {
            String keyword = leadingKeyword(statement);
            if (isDdlHead(keyword)) {
                String trimmed = statement.trim();
                ddlOnly.append(trimmed);
                if (!trimmed.endsWith(";")) {
                    ddlOnly.append(";");
                }
                ddlOnly.append("\n");
            }
        }
        return ddlOnly.toString();
    }

    /* --- A: Remove PG/OG COPY data segments --- */
    private static String stripPgCopyData(String sql) {
        StringBuilder out = new StringBuilder(sql.length());
        try (BufferedReader br = new BufferedReader(new StringReader(sql))) {
            String line;
            boolean isSkippingCopyData = false;
            while ((line = br.readLine()) != null) {
                String trim = line.trim();
                if (!isSkippingCopyData) {
                    if (trim.regionMatches(true, 0, "COPY ", 0, 5)
                            && trim.toUpperCase(Locale.ROOT).contains("FROM STDIN")) {
                        isSkippingCopyData = true;
                        continue;
                    } else {
                        out.append(line).append("\n");
                    }
                } else if ("\\.".equals(trim)) {
                    isSkippingCopyData = false;
                } else {
                    out.append(line).append("\n");
                }
            }
        } catch (IOException ex) {
            // Preserve existing behaviour: retain COPY contents when parsing fails
            return sql;
        }
        return out.toString();
    }

    /* --- B: Split statements (aware of quotes/comments/dollar/backtick) --- */
    private static List<String> splitSqlStatements(String sql) {
        return new StatementSplitter(sql).split();
    }

    private static final class StatementSplitter {
        private final String source;
        private final List<String> statements = new ArrayList<>();
        private final StringBuilder buffer;

        private boolean isInSingleQuote;
        private boolean isInDoubleQuote;
        private boolean isInBacktick;
        private boolean isInLineComment;
        private boolean isInBlockComment;
        private String activeDollarTag;
        private char previousChar;

        StatementSplitter(String source) {
            this.source = source;
            this.buffer = new StringBuilder(source.length());
        }

        List<String> split() {
            for (int index = 0; index < source.length(); index++) {
                char current = source.charAt(index);
                char next = nextChar(index);

                handleCommentTransitions(current, next);
                if (isInLineComment) {
                    appendAndMaybeExitLineComment(current);
                    continue;
                }
                if (isInBlockComment) {
                    appendAndTrack(current);
                    continue;
                }

                handleDollarStrings(index, current);
                toggleQuoteStates(current);

                if (shouldSplitOn(current)) {
                    buffer.append(current);
                    statements.add(buffer.toString());
                    buffer.setLength(0);
                    previousChar = current;
                    continue;
                }

                appendAndTrack(current);
            }

            String remaining = buffer.toString().trim();
            if (!remaining.isEmpty()) {
                statements.add(remaining);
            }
            return statements;
        }

        private char nextChar(int index) {
            return index + 1 < source.length() ? source.charAt(index + 1) : 0;
        }

        private void handleCommentTransitions(char current, char next) {
            if (isInDollarOrQuoted()) {
                return;
            }
            if (!isInLineComment && !isInBlockComment && current == '-' && next == '-') {
                isInLineComment = true;
            } else if (!isInLineComment && !isInBlockComment && current == '#') {
                isInLineComment = true;
            } else if (!isInBlockComment && current == '/' && next == '*') {
                isInBlockComment = true;
            } else if (isInBlockComment && previousChar == '*' && current == '/') {
                isInBlockComment = false;
            } else {
                ; // Not a comment marker, continue processing
            }
        }

        private boolean isInDollarOrQuoted() {
            return activeDollarTag != null || isInSingleQuote || isInDoubleQuote || isInBacktick;
        }

        private void appendAndMaybeExitLineComment(char current) {
            appendAndTrack(current);
            if (current == '\n') {
                isInLineComment = false;
            }
        }

        private void appendAndTrack(char current) {
            buffer.append(current);
            previousChar = current;
        }

        private void handleDollarStrings(int index, char current) {
            if (isInSingleQuote || isInDoubleQuote || isInBacktick) {
                return;
            }
            if (activeDollarTag == null && current == '$') {
                probeDollarTag(index).ifPresent(tag -> activeDollarTag = tag);
            } else if (activeDollarTag != null && current == '$' && matchesClosingDollarTag(index)) {
                activeDollarTag = null;
            } else {
                ; // Not a dollar tag delimiter, continue processing
            }
        }

        private Optional<String> probeDollarTag(int start) {
            int probe = start + 1;
            while (probe < source.length() && isDollarIdent(source.charAt(probe))) {
                probe++;
            }
            if (probe < source.length() && source.charAt(probe) == '$') {
                return Optional.of(source.substring(start, probe + 1));
            }
            return Optional.empty();
        }

        private boolean matchesClosingDollarTag(int index) {
            int tagLength = activeDollarTag.length();
            int start = index - (tagLength - 1);
            if (start < 0) {
                return false;
            }
            String candidate = source.substring(start, index + 1);
            return candidate.equals(activeDollarTag);
        }

        private void toggleQuoteStates(char current) {
            if (activeDollarTag != null) {
                return;
            }
            if (!isInDoubleQuote && !isInBacktick && current == '\'' && previousChar != '\\') {
                isInSingleQuote = !isInSingleQuote;
            } else if (!isInSingleQuote && !isInBacktick && current == '"' && previousChar != '\\') {
                isInDoubleQuote = !isInDoubleQuote;
            } else if (!isInSingleQuote && !isInDoubleQuote && current == '`' && previousChar != '\\') {
                isInBacktick = !isInBacktick;
            } else {
                ; // Not a quote character, continue processing
            }
        }

        private boolean shouldSplitOn(char current) {
            return current == ';'
                    && !isInSingleQuote
                    && !isInDoubleQuote
                    && !isInBacktick
                    && activeDollarTag == null;
        }
    }

    private static boolean isDollarIdent(char candidate) {
        return Character.isLetterOrDigit(candidate) || candidate == '_';
    }

    private static String leadingKeyword(String statement) {
        if (statement == null) {
            return "";
        }
        String remaining = statement.trim();

        boolean isStrippingComments = true;
        while (isStrippingComments && !remaining.isEmpty()) {
            if (remaining.startsWith("--")) {
                int newline = remaining.indexOf('\n');
                remaining = newline >= 0 ? remaining.substring(newline + 1).trim() : "";
            } else if (remaining.startsWith("#")) {
                int newline = remaining.indexOf('\n');
                remaining = newline >= 0 ? remaining.substring(newline + 1).trim() : "";
            } else if (remaining.startsWith("/*")) {
                int end = remaining.indexOf("*/");
                remaining = end >= 0 ? remaining.substring(end + 2).trim() : "";
            } else {
                isStrippingComments = false;
            }
        }

        if (remaining.isEmpty()) {
            return "";
        }
        String[] parts = remaining.toUpperCase(Locale.ROOT).split("\\s+");
        if (parts.length == 0) {
            return "";
        }
        if (parts.length >= 2) {
            return parts[0] + " " + parts[1];
        }
        return parts[0];
    }

    private static boolean isDdlHead(String headUpper) {
        if (headUpper == null) {
            return false;
        }
        if (headUpper.startsWith("CREATE TABLE")) {
            return true;
        }
        if (headUpper.startsWith("ALTER TABLE")) {
            return true;
        }
        if (headUpper.startsWith("CREATE INDEX")) {
            return true;
        }
        if (headUpper.startsWith("CREATE UNIQUE")) {
            return true;
        }
        return false;
    }
}
