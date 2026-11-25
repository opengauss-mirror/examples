package org.opengauss.batch;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 补测三：监听器族——StepExecutionListener / ChunkListener / SkipListener / ItemProcessListener
 * 在同一作业中同时生效（此前仅覆盖 JobExecutionListener）。
 *
 * <p>数据：5 行含 2 个坏行（SKIP_ME），chunk=2 + skip 容错。带坏行的 chunk 首次处理
 * 整体失败回滚（TaskletStep 重试该 chunk），重试时坏行被 skip、好行重新处理——
 * 因此 ItemProcessListener.afterProcess 对好行每次尝试各触发一次
 * （good1/good2 各 2 次 + good3 1 次 = 5），ChunkListener 的 before/after 只在
 * 成功路径成对（5 次 before / 3 次 after）。各计数断言如下。</p>
 */
@SpringBootTest
class MoreListenersTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("moreListenersJob")
    private Job moreListenersJob;

    @Autowired
    private AtomicInteger stepBeforeCount;

    @Autowired
    private AtomicInteger stepAfterCount;

    @Autowired
    private AtomicInteger chunkBeforeCount;

    @Autowired
    private AtomicInteger chunkAfterCount;

    @Autowired
    private AtomicInteger processAfterCount;

    @Autowired
    private CopyOnWriteArrayList<String> processedNames;

    @Autowired
    private CopyOnWriteArrayList<String> skippedNames;

    @Autowired
    private CopyOnWriteArrayList<String> stepNamesSeen;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        stepBeforeCount.set(0);
        stepAfterCount.set(0);
        chunkBeforeCount.set(0);
        chunkAfterCount.set(0);
        processAfterCount.set(0);
        processedNames.clear();
        skippedNames.clear();
        stepNamesSeen.clear();

        jdbc.update("DROP TABLE IF EXISTS src_listen");
        jdbc.update("DROP TABLE IF EXISTS dst_listen");
        jdbc.update("CREATE TABLE src_listen (id INT PRIMARY KEY, name VARCHAR(50), age INT)");
        jdbc.update("CREATE TABLE dst_listen (id INT PRIMARY KEY, name VARCHAR(50), age INT)");
        String[] names = {"good1", "SKIP_ME", "good2", "SKIP_ME", "good3"};
        for (int i = 0; i < names.length; i++) {
            jdbc.update("INSERT INTO src_listen (id, name, age) VALUES (?, ?, ?)", i + 1, names[i], 20 + i);
        }
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DROP TABLE IF EXISTS src_listen");
        jdbc.update("DROP TABLE IF EXISTS dst_listen");
    }

    @Test
    void allListenerFamiliesFire() throws Exception {
        JobExecution ex = jobLauncher.run(moreListenersJob, new JobParametersBuilder()
                .addString("run.id", "listeners-" + System.currentTimeMillis()).toJobParameters());

        assertEquals(BatchStatus.COMPLETED, ex.getStatus());
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM dst_listen", Integer.class));

        // 先打印全部计数再断言（排查全量/单测环境下 skip 行为差异用）
        System.out.println("[DEBUG] stepBefore=" + stepBeforeCount.get()
                + ", stepAfter=" + stepAfterCount.get()
                + ", chunk=" + chunkBeforeCount.get() + "/" + chunkAfterCount.get()
                + ", skipped=" + skippedNames
                + ", processed=" + processAfterCount.get()
                + ", names=" + processedNames);

        // StepExecutionListener：before/after 各一次，step 名正确
        assertEquals(1, stepBeforeCount.get());
        assertEquals(1, stepAfterCount.get());
        assertTrue(stepNamesSeen.contains("moreListenersStep"), "beforeStep 应看到步骤名，实际 " + stepNamesSeen);

        // SkipListener：2 个坏行按处理顺序记录，仅在坏行真正被 skip 时触发
        assertEquals(List.of("SKIP_ME", "SKIP_ME"), skippedNames);

        // ItemProcessListener：带坏行的 chunk 先整体失败回滚、重试时好行重新处理，
        // 故 good1/good2 各触发 2 次、good3 一次成功 1 次，坏行从不触发 afterProcess
        assertEquals(5, processAfterCount.get());
        assertEquals(List.of("good1", "good1", "good2", "good2", "good3"), processedNames);

        // ChunkListener：before 每次 chunk 尝试各触发一次（3 chunk = 2 个带坏行重试 + 1 个干净 = 5 次），
        // after 仅在 chunk 成功提交时触发（3 次）——失败回滚的尝试不触发 afterChunk
        assertEquals(5, chunkBeforeCount.get());
        assertEquals(3, chunkAfterCount.get());

        System.out.println("[CASE] 监听器族齐活: stepBefore=" + stepBeforeCount.get()
                + ", chunk=" + chunkBeforeCount.get() + "/" + chunkAfterCount.get()
                + ", skipped=" + skippedNames + ", processed=" + processAfterCount.get()
                + ", names=" + processedNames);
    }
}
