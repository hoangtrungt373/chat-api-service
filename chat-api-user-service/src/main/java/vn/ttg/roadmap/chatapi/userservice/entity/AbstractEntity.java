package vn.ttg.roadmap.chatapi.userservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import vn.ttg.roadmap.chatapi.userservice.entity.generator.TableNameSequenceGenerator;
import vn.ttg.roadmap.chatapi.userservice.util.DateUtils;
import vn.ttg.roadmap.chatapi.userservice.util.UserUtils;

import java.time.Instant;

/**
 * Abstract base entity with automatic sequence generation based on table name.
 * 
 * Child entities automatically use sequence: schema.TABLE_NAME_SEQ
 * based on their @Table annotation. No need to declare @GenericGenerator.
 * 
 * Example:
 * @Entity
 * @Table(name = "USER", schema = "product")
 * public class User extends AbstractEntity {
 *     // Automatically uses: product.USER_SEQ
 * }
 * 
 * @author ttg
 */
@MappedSuperclass
@Data
public abstract class AbstractEntity {
    
    @Id
    @GenericGenerator(
        name = "sequence_generator",
        type = TableNameSequenceGenerator.class,
        parameters = {
            @Parameter(name = "schema", value = "product"),
            @Parameter(name = "allocation_size", value = "50")
        }
    )
    @GeneratedValue(generator = "sequence_generator", strategy = GenerationType.SEQUENCE)
    @NotNull
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Size(max = 128)
    @Column(name = "USR_CREATION", length = 128, nullable = false)
    private String usrCreation;

    @NotNull
    @CreationTimestamp
    @Column(name = "DTE_CREATION", nullable = false, updatable = false)
    private Instant dteCreation;

    @NotNull
    @Size(max = 128)
    @Column(name = "USR_LAST_MODIFICATION", length = 128, nullable = false)
    private String usrLastModification;

    @NotNull
    @UpdateTimestamp
    @Column(name = "DTE_LAST_MODIFICATION", nullable = false)
    private Instant dteLastModification;

    @Version
    @Column(name = "VERSION")
    private Integer version;
    
    /**
     * Set audit fields before persisting
     */
    @PrePersist
    protected void beforeSave() {
        setUsrCreation(UserUtils.getUserName());
        setDteCreation(DateUtils.getCurrentDateTime());
        setUsrLastModification(UserUtils.getUserName());
        setDteLastModification(DateUtils.getCurrentDateTime());
    }
    
    /**
     * Update modification fields before updating
     */
    @PreUpdate
    protected void beforeUpdate() {
        setUsrLastModification(UserUtils.getUserName());
        setDteLastModification(DateUtils.getCurrentDateTime());
    }
}
