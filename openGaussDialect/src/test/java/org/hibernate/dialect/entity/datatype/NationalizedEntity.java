package org.hibernate.dialect.entity.datatype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "nationalized_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NationalizedEntity {
    @Id
    private Long id;
    @Nationalized
    private String nvarcharData;
    @Nationalized
    private Character ncharData;
    @Lob
    @Nationalized
    private String nclobData;
}