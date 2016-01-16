package ch.softappeal.yass.tutorial.initiator.web;

import ch.softappeal.yass.ts.ContractGenerator;
import ch.softappeal.yass.tutorial.contract.Config;

import java.util.HashMap;
import java.util.Map;

public final class GenerateTypeScriptContract {

    public static void main(final String... args) throws Exception {
        final Map<Class<?>, String> java2tsBaseType = new HashMap<>();
        java2tsBaseType.put(Integer.class, "Integer");
        new ContractGenerator(
            Config.class.getPackage().getName(), Config.SERIALIZER, Config.INITIATOR, Config.ACCEPTOR,
            "baseTypes", "contract", java2tsBaseType, "src/tutorial/ts/contract.ts"
        );
    }

}
