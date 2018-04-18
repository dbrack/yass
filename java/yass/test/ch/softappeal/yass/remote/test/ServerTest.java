package ch.softappeal.yass.remote.test;

import ch.softappeal.yass.remote.Client;
import ch.softappeal.yass.remote.ContractId;
import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.remote.Service;
import ch.softappeal.yass.remote.TaggedMethodMapper;
import ch.softappeal.yass.test.InvokeTest;
import org.junit.Assert;
import org.junit.Test;

public class ServerTest {

    static final Client CLIENT = new Client() {
        @Override public void invoke(final Client.Invocation invocation) throws Exception {
            invocation.invoke(
                false,
                request -> new Server(new Service(ContractIdTest.ID, new InvokeTest.TestServiceImpl()))
                    .invocation(false, request).invoke(invocation::settle)
            );
        }
    };

    @Test public void duplicatedService() {
        final var service = new Service(ContractIdTest.ID, new InvokeTest.TestServiceImpl());
        try {
            new Server(service, service);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("serviceId 987654 already added", e.getMessage());
        }
    }

    @Test public void noService() {
        try {
            CLIENT.proxy(ContractId.create(InvokeTest.TestService.class, 123456, TaggedMethodMapper.FACTORY)).nothing();
            Assert.fail();
        } catch (final RuntimeException e) {
            Assert.assertEquals("no serviceId 123456 found (methodId 0)", e.getMessage());
        }
    }

}
