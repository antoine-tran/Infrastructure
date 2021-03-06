package tests.eu.qualimaster.monitoring;

import java.util.Properties;
import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.monitoring.MonitoringConfiguration;
import tests.eu.qualimaster.coordination.CoordinationConfigurationTests;

/**
 * Tests the configuration.
 * 
 * @author Holger Eichelberger
 */
public class MonitoringConfigurationTests extends CoordinationConfigurationTests {
    
    @Override
    protected void testDirect() {
        super.testDirect();
        Assert.assertEquals("/var/log", MonitoringConfiguration.getMonitoringLogLocation());
        Assert.assertEquals(MonitoringConfiguration.DEFAULT_MONITORING_LOG_INFRA_LOCATION, 
            MonitoringConfiguration.getMonitoringLogInfraLocation());
        // falls back to MonitoringLogLocation as not configured 
        Assert.assertEquals("/var/log", MonitoringConfiguration.getProfilingLogLocation());
        Assert.assertEquals(MonitoringConfiguration.DEFAULT_MONITORING_NODE_FREQUENCY, 
            MonitoringConfiguration.getMonitoringNodeFrequency());
        Assert.assertEquals(1001, MonitoringConfiguration.getClusterMonitoringFrequency());
        Assert.assertEquals(1002, MonitoringConfiguration.getPipelineMonitoringFrequency());
        Assert.assertEquals(5, MonitoringConfiguration.getStormExecutorStartupWaitingTime());
        Assert.assertTrue(MonitoringConfiguration.debugThriftMonitoring());
        Assert.assertEquals(MonitoringConfiguration.DEFAULT_VOLUME_MODEL_LOCATION, 
            MonitoringConfiguration.getProfileLocation());
    }

    @Override
    protected void testAfterReplay() {
        super.testAfterReplay();
        assertSet(toList("capacity"), MonitoringConfiguration.getMonitoringAnalysisDisabled());
        assertSet(toList("okeanos1", "okeanos2"), MonitoringConfiguration.getMonitoringHardwareFilter());
    }
    
    @Override
    protected void buildProperties(Properties prop) {
        super.buildProperties(prop);
    }
    
    @Override
    protected void testViaProperties() {
        super.testViaProperties();
    }
    
    @Override
    @Test
    public void configurationTest() {
        super.configurationTest();
        System.setProperty("qm.reasoning", "true");
        Assert.assertTrue(MonitoringConfiguration.isReasoningEnabled());
        System.setProperty("qm.reasoning", "false");
        Assert.assertFalse(MonitoringConfiguration.isReasoningEnabled());
    }
    
}
