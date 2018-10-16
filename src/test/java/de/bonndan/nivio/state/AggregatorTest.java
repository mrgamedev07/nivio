package de.bonndan.nivio.state;

import de.bonndan.nivio.ProcessingException;
import de.bonndan.nivio.input.dto.Environment;
import de.bonndan.nivio.landscape.FullyQualifiedIdentifier;
import de.bonndan.nivio.landscape.StateProviderConfig;
import de.bonndan.nivio.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AggregatorTest {

    @Mock
    NotificationService notificationService;

    @Mock
    ProviderFactory factory;

    Map<FullyQualifiedIdentifier, ServiceState> state;

    private Aggregator aggregator;


    @BeforeEach
    void configureSystemUnderTest() {
        initMocks(this);
        state = new HashMap<>();

        aggregator = new Aggregator(state, factory, notificationService);
    }

    @Test
    public void testWithFactoryErrors() {

        Environment e = new Environment();
        e.setIdentifier("test");
        e.setStateProviders(List.of(new StateProviderConfig("type", "target")));

        Mockito.when(factory.createFor(Mockito.any(), Mockito.any())).thenThrow(new ProcessingException(e, "fail"));

        aggregator.fetch(e);
        Mockito.verify(notificationService, Mockito.atLeastOnce()).sendError(Mockito.any(), Mockito.any());
    }

    @Test
    public void testAppliesState() {

        Environment e = new Environment();
        e.setIdentifier("test");
        e.setStateProviders(List.of(new StateProviderConfig(StateProviderConfig.TYPE_PROMETHEUS_EXPORTER, "target")));

        FullyQualifiedIdentifier fullyQualifiedIdentifier = new FullyQualifiedIdentifier("x", "y", "z");
        Provider mockProvider = Mockito.mock(Provider.class);
        when(mockProvider.getStates()).then(invocationOnMock -> {
            Map<FullyQualifiedIdentifier, ServiceState> updates = new HashMap<>();
            updates.put(fullyQualifiedIdentifier, new ServiceState(Level.ERROR, "error"));
            return updates;
        });

        Mockito.when(factory.createFor(Mockito.any(), Mockito.any())).thenReturn(mockProvider);

        aggregator.fetch(e);
        assertFalse(state.isEmpty());
        assertEquals(1, state.size());
        ServiceState s1 = state.get(fullyQualifiedIdentifier);
        assertNotNull(s1);
        assertEquals(Level.ERROR, s1.getLevel());
        assertEquals("error", s1.getMessage());
    }

    @Test
    public void testServiceDeteriorates() {

        Environment e = new Environment();
        e.setIdentifier("test");
        e.setStateProviders(List.of(new StateProviderConfig(StateProviderConfig.TYPE_PROMETHEUS_EXPORTER, "target")));
        FullyQualifiedIdentifier fullyQualifiedIdentifier = new FullyQualifiedIdentifier("x", "y", "z");
        state.put(fullyQualifiedIdentifier, new ServiceState(Level.OK, "good"));


        Provider mockProvider = Mockito.mock(Provider.class);
        when(mockProvider.getStates()).then(invocationOnMock -> {
            Map<FullyQualifiedIdentifier, ServiceState> updates = new HashMap<>();
            updates.put(fullyQualifiedIdentifier, new ServiceState(Level.ERROR, "error"));
            return updates;
        });

        Mockito.when(factory.createFor(Mockito.any(), Mockito.any())).thenReturn(mockProvider);

        aggregator.fetch(e);
        assertEquals(1, state.size());
        ServiceState s1 = state.get(fullyQualifiedIdentifier);
        assertNotNull(s1);
        assertEquals(Level.ERROR, s1.getLevel());
        assertEquals("error", s1.getMessage());
    }

    @Test
    public void testServiceRecovers() {

        Environment e = new Environment();
        e.setIdentifier("test");
        e.setStateProviders(List.of(new StateProviderConfig(StateProviderConfig.TYPE_PROMETHEUS_EXPORTER, "target")));
        FullyQualifiedIdentifier fullyQualifiedIdentifier = new FullyQualifiedIdentifier("x", "y", "z");
        state.put(fullyQualifiedIdentifier, new ServiceState(Level.ERROR, "error"));


        Provider mockProvider = Mockito.mock(Provider.class);
        when(mockProvider.getStates()).then(invocationOnMock -> {
            Map<FullyQualifiedIdentifier, ServiceState> updates = new HashMap<>();
            updates.put(fullyQualifiedIdentifier, new ServiceState(Level.WARNING, "better"));
            return updates;
        });

        Mockito.when(factory.createFor(Mockito.any(), Mockito.any())).thenReturn(mockProvider);

        aggregator.fetch(e);
        assertEquals(1, state.size());
        ServiceState s1 = state.get(fullyQualifiedIdentifier);
        assertNotNull(s1);
        assertEquals(Level.WARNING, s1.getLevel());
        assertEquals("better", s1.getMessage());
    }
}