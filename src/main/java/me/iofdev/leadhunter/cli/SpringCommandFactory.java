package me.iofdev.leadhunter.cli;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

/**
 * Lets picocli build commands through Spring, so commands get their dependencies
 * by constructor injection without being registered as beans. See ADR 0009.
 */
@Component
class SpringCommandFactory implements CommandLine.IFactory {

    private final ApplicationContext context;

    SpringCommandFactory(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public <K> K create(Class<K> type) throws Exception {
        if (type.isAnnotationPresent(CommandLine.Command.class)) {
            return context.getAutowireCapableBeanFactory().createBean(type);
        }
        return CommandLine.defaultFactory().create(type);
    }
}
