package org.hibernate.dialect.entity.datatype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;

@Entity
@Table(name = "lob_test_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LobEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Lob
    private Blob blobField; // Blob --> blob
    @Lob
    private Clob clobField; // Clob -->  clob
    @Lob
    private NClob nclobField; // NClob --> text
}
