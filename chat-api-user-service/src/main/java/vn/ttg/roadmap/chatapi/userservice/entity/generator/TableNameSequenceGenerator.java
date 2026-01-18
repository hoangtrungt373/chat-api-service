package vn.ttg.roadmap.chatapi.userservice.entity.generator;

import jakarta.persistence.Table;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.type.Type;

import java.util.Properties;

/**
 * Custom sequence generator that automatically generates sequence names
 * based on the table name in the format: schema.TABLE_NAME_SEQ
 * 
 * Automatically detects table name from @Table annotation or entity metadata.
 * Child entities do NOT need to specify table_name parameter.
 * 
 * Usage in AbstractEntity (only once):
 * @GenericGenerator(
 *     name = "sequence_generator",
 *     type = TableNameSequenceGenerator.class,
 *     parameters = {
 *         @Parameter(name = "schema", value = "product"),
 *         @Parameter(name = "allocation_size", value = "50")
 *     }
 * )
 * @GeneratedValue(generator = "sequence_generator", strategy = GenerationType.SEQUENCE)
 * 
 * Child entities just need @Table annotation:
 * @Entity
 * @Table(name = "USER", schema = "product")
 * public class User extends AbstractEntity {
 *     // Automatically uses: product.USER_SEQ
 * }
 */
public class TableNameSequenceGenerator extends SequenceStyleGenerator {
    
    private static final String DEFAULT_SCHEMA = "product";
    private static final String SEQUENCE_SUFFIX = "_SEQ";
    private static final String DEFAULT_ALLOCATION_SIZE = "50";
    private static final String DEFAULT_INITIAL_VALUE = "1";
    
    private String resolvedSequenceName;
    
    @Override
    public void configure(Type type, Properties params, 
                         org.hibernate.service.ServiceRegistry serviceRegistry) {
        String tableName = params.getProperty("table_name");
        String schema = params.getProperty("schema", DEFAULT_SCHEMA);
        String allocationSize = params.getProperty("allocation_size", DEFAULT_ALLOCATION_SIZE);
        String initialValue = params.getProperty("initial_value", DEFAULT_INITIAL_VALUE);
        
        // If table_name is not provided, it will be resolved at runtime
        if (tableName != null && !tableName.isEmpty()) {
            // Generate sequence name: schema.TABLE_NAME_SEQ
            resolvedSequenceName = schema + "." + tableName + SEQUENCE_SUFFIX;
            params.setProperty(SEQUENCE_PARAM, resolvedSequenceName);
        } else {
            // Will be resolved at first generation using entity metadata
            // Store schema for later use
            params.setProperty("_schema", schema);
        }
        
        // Set default values if not provided
        if (!params.containsKey(INITIAL_PARAM)) {
            params.setProperty(INITIAL_PARAM, initialValue);
        }
        if (!params.containsKey(INCREMENT_PARAM)) {
            params.setProperty(INCREMENT_PARAM, allocationSize);
        }
        if (!params.containsKey(OPT_PARAM)) {
            params.setProperty(OPT_PARAM, "legacy-hilo");
        }
        
        // Call parent configure (will use resolvedSequenceName if set)
        super.configure(type, params, serviceRegistry);
    }
    
    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        // If sequence name wasn't resolved during configuration, resolve it now
        if (resolvedSequenceName == null) {
            resolveSequenceName(session, object);
        }
        
        return super.generate(session, object);
    }
    
    private void resolveSequenceName(SharedSessionContractImplementor session, Object object) {
        try {
            Class<?> entityClass = object.getClass();
            
            // Extract table name and schema from @Table annotation
            String tableName = extractTableNameFromAnnotation(entityClass);
            String schema = extractSchemaFromAnnotation(entityClass);
            
            // If table name not found in annotation, use entity class simple name as fallback
            if (tableName == null || tableName.isEmpty()) {
                tableName = entityClass.getSimpleName().toUpperCase();
            }
            
            // If schema is null, use default
            if (schema == null || schema.isEmpty()) {
                schema = DEFAULT_SCHEMA;
            }
            
            // Remove schema prefix from table name if present
            if (tableName.contains(".")) {
                String[] parts = tableName.split("\\.");
                if (parts.length == 2) {
                    schema = parts[0];
                    tableName = parts[1];
                } else {
                    tableName = parts[parts.length - 1];
                }
            }
            
            // Generate sequence name: schema.TABLE_NAME_SEQ
            resolvedSequenceName = schema + "." + tableName + SEQUENCE_SUFFIX;
            
            // Update the sequence parameter for parent class
            Properties params = new Properties();
            params.setProperty(SEQUENCE_PARAM, resolvedSequenceName);
            params.setProperty(INITIAL_PARAM, "1");
            params.setProperty(INCREMENT_PARAM, "50");
            params.setProperty(OPT_PARAM, "legacy-hilo");
            
            // Reconfigure with resolved sequence name
            super.configure(
                session.getFactory().getTypeConfiguration()
                    .getBasicTypeRegistry()
                    .getRegisteredType(Integer.class.getName()),
                params,
                session.getFactory().getServiceRegistry()
            );
            
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to resolve sequence name for entity: " + object.getClass().getName() + 
                ". Please ensure @Table annotation is present with 'name' attribute or specify table_name parameter in @GenericGenerator.", e);
        }
    }
    
    private String extractTableNameFromAnnotation(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null) {
            String name = tableAnnotation.name();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        return null;
    }
    
    private String extractSchemaFromAnnotation(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.schema().isEmpty()) {
            return tableAnnotation.schema();
        }
        return null;
    }
}
