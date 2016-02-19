package ch.softappeal.yass.tutorial.initiator.web;

import ch.softappeal.yass.generate.TypeScriptGenerator;
import ch.softappeal.yass.tutorial.contract.Config;

import java.util.HashMap;
import java.util.Map;

public final class GenerateTypeScript {

    public static void main(final String... args) throws Exception {
        final Map<Class<?>, TypeScriptGenerator.TypeDesc> externalTypes = new HashMap<>();
        externalTypes.put(Integer.class, new TypeScriptGenerator.TypeDesc("Integer", "IntegerHandler")); // shows how to use a contract external base type
        new TypeScriptGenerator(
            Config.class.getPackage().getName(), Config.CONTRACT_SERIALIZER, Config.INITIATOR, Config.ACCEPTOR,
            "src/tutorial/ts/namespace/contract-include.txt", "contract", externalTypes, "src/tutorial/ts/namespace/contract.ts"
        );
        new TypeScriptGenerator(
            Config.class.getPackage().getName(), Config.CONTRACT_SERIALIZER, Config.INITIATOR, Config.ACCEPTOR,
            "src/tutorial/ts/module/contract-include.txt", null, externalTypes, "src/tutorial/ts/module/contract.ts"
        );
    }

}
