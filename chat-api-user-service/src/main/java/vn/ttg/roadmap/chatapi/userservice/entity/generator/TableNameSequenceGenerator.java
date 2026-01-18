package vn.ttg.roadmap.chatapi.userservice.entity.generator;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.OptimizerDescriptor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import jakarta.persistence.Table;


public class TableNameSequenceGenerator extends SequenceStyleGenerator {

    private static final String ENTITY_TYPE_CLASS = "entity-type-class";

    @Override
    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
        try {
            params.put(ENTITY_TYPE_CLASS, Class.forName(ConfigurationHelper.getString(IdentifierGenerator.ENTITY_NAME, params)));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        super.configure(type, params, serviceRegistry);
    }

    @Override
    protected QualifiedName determineSequenceName(Properties params, Dialect dialect, JdbcEnvironment jdbcEnv, ServiceRegistry serviceRegistry) {
        String tableName = ConfigurationHelper.getString(PersistentIdentifierGenerator.TABLE, params);

        // Default suffix: "_SEQ" (safe even if Hibernate defaults change)
        String suffix = ConfigurationHelper.getString(
                SequenceStyleGenerator.CONFIG_SEQUENCE_PER_ENTITY_SUFFIX,
                params,
                "_SEQ"
        );

        String defaultSequenceName = tableName + suffix;

        // Allow override via @GenericGenerator(parameters = @Parameter(name="sequence_name", ...))
        String sequenceName = ConfigurationHelper.getString(SEQUENCE_PARAM, params, defaultSequenceName);

        if (sequenceName.contains(".")) {
            return QualifiedNameParser.INSTANCE.parse(sequenceName);
        }

        Identifier catalog = jdbcEnv.getIdentifierHelper().toIdentifier(
                ConfigurationHelper.getString(PersistentIdentifierGenerator.CATALOG, params)
        );

        Identifier schema = jdbcEnv.getIdentifierHelper().toIdentifier(
                ConfigurationHelper.getString(PersistentIdentifierGenerator.SCHEMA, params)
        );

        if (schema == null) {
            Table tableAnnotation = ((Class<?>) params.get(ENTITY_TYPE_CLASS)).getAnnotation(Table.class);
            if (tableAnnotation != null && tableAnnotation.schema() != null && !tableAnnotation.schema().trim().isEmpty()) {
                schema = jdbcEnv.getIdentifierHelper().toIdentifier(tableAnnotation.schema().trim());
            }
        }

        return new QualifiedNameParser.NameParts(
                catalog,
                schema,
                jdbcEnv.getIdentifierHelper().toIdentifier(sequenceName)
        );
    }

    @Override
    protected OptimizerDescriptor determineOptimizationStrategy(Properties params, int incrementSize) {
        // Default to pooled-lo. If OPT_PARAM is set to a known optimizer, honor it.
        String optimizerName = ConfigurationHelper.getString(
                OPT_PARAM,
                params,
                StandardOptimizerDescriptor.POOLED_LO.getExternalName()
        );

        for (StandardOptimizerDescriptor d : StandardOptimizerDescriptor.values()) {
            if (d.getExternalName().equalsIgnoreCase(optimizerName)) {
                return d;
            }
        }

        // Unknown/custom optimizer string: just fall back safely
        return StandardOptimizerDescriptor.POOLED_LO;
    }
}
