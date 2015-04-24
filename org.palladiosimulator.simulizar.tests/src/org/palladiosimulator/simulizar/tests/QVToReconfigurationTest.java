package org.palladiosimulator.simulizar.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.Test;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.monitorrepository.MonitorRepositoryFactory;
import org.palladiosimulator.monitorrepository.StatisticalCharacterizationEnum;
import org.palladiosimulator.runtimemeasurement.RuntimeMeasurement;
import org.palladiosimulator.runtimemeasurement.RuntimeMeasurementFactory;
import org.palladiosimulator.simulizar.access.IModelAccess;
import org.palladiosimulator.simulizar.access.ModelAccess;
import org.palladiosimulator.simulizar.reconfiguration.qvto.QVTOExecutor;
import org.palladiosimulator.simulizar.runconfig.SimuLizarWorkflowConfiguration;

import de.uka.ipd.sdq.pcm.allocation.Allocation;
import de.uka.ipd.sdq.pcm.allocation.AllocationContext;
import de.uka.ipd.sdq.pcm.allocation.util.AllocationResourceFactoryImpl;
import de.uka.ipd.sdq.pcm.core.composition.AssemblyConnector;
import de.uka.ipd.sdq.pcm.core.composition.AssemblyContext;
import de.uka.ipd.sdq.pcm.core.composition.Connector;
import de.uka.ipd.sdq.pcm.repository.BasicComponent;
import de.uka.ipd.sdq.pcm.repository.util.RepositoryResourceFactoryImpl;
import de.uka.ipd.sdq.pcm.resourceenvironment.ProcessingResourceSpecification;
import de.uka.ipd.sdq.pcm.resourceenvironment.ResourceContainer;
import de.uka.ipd.sdq.pcm.resourceenvironment.ResourceEnvironment;
import de.uka.ipd.sdq.pcm.resourceenvironment.util.ResourceenvironmentResourceFactoryImpl;
import de.uka.ipd.sdq.pcm.seff.ProbabilisticBranchTransition;
import de.uka.ipd.sdq.pcm.seff.ResourceDemandingSEFF;
import de.uka.ipd.sdq.pcm.seff.ServiceEffectSpecification;
import de.uka.ipd.sdq.pcm.system.util.SystemResourceFactoryImpl;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import de.uka.ipd.sdq.workflow.pcm.blackboard.PCMResourceSetPartition;
import de.uka.ipd.sdq.workflow.pcm.jobs.LoadPCMModelsIntoBlackboardJob;

public class QVToReconfigurationTest {

    private final String PROJECT_NAME = "org.palladiosimulator.simulizar.tests";
    private final String REPOSITORY_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/server.repository";
    private final String RESOURCE_ENVIRONMENT_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/server.resourceenvironment";
    private final String SYSTEM_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/server.system";
    private final String ALLOCATION_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/server.allocation";
    private final String PMS_MODEL_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/server.pms";
    private final String TRANSFORMATION_RULES_ADD_DUPLICATED_SERVER_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/rules/addClonedServer";
    private final String TRANSFORMATION_RULES_ADD_SERVER_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/rules/addNewServer";
    private final String TRANSFORMATION_RULES_OUTSOURCE_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/rules/outsource";
    private final String TRANSFORMATION_RULES_SCALE_UP_PATH = "/org.palladiosimulator.simulizar.tests/testmodel/rules/scaleUp";
    private final String PMS_CONFIGURATION_KEY = "pmsFile";
    private final String RECONFIGURATION_RULES_CONFIGURATION_KEY = "reconfigurationRulesFolder";
    private final String ALLOCATION_FILE_CONFIGURATION_KEY = "allocationFile";
    private final String REPOSITORY_EXTENSION = "repository";
    private final String RESOURCE_ENVIRONMENT_EXTENSION = "resourceenvironment";
    private final String SYSTEM_EXTENSION = "system";
    private final String ALLOCATION_EXTENSION = "allocation";
    private final String BRANCH_2_ENTITY_NAME = "branch2";
    private final double BRANCH_2_EXPECTED_VALUE_AFTER_OUTSOURCING = 0.1;
    private final double BRANCH_2_EXPECTED_VALUE_BEFORE_OUTSOURCING = 0.0;
    private final double MEASUREMENT_BELOW_THRESHOLD = 1.0;
    private final double MEASUREMENT_OVER_THRESHOLD = 5.0;
    private final String SERVER_RESOURCE_CONTAINER_NAME = "server";
    private final double SERVER_EXPECTED_PROCESSING_RATE_AFTER_SCALING = 1100.0;
    private final double SERVER_EXPECTED_PROCESSING_RATE_BEFORE_SCALING = 1000.0;
    private final int EXPECTED_NUMBER_OF_SERVERS_BEFORE_ADDING = 1;
    private final int EXPECTED_NUMBER_OF_SERVERS_AFTER_ADDING = 2;

    @Test
    public void test() {
        assertEquals("The branch probability was not changed as expected!", BRANCH_2_EXPECTED_VALUE_AFTER_OUTSOURCING,
                outsource(MEASUREMENT_OVER_THRESHOLD), 0.0);
        assertEquals("The branch probability has not remained as it was expected!",
                BRANCH_2_EXPECTED_VALUE_BEFORE_OUTSOURCING, outsource(MEASUREMENT_BELOW_THRESHOLD), 0.0);

        assertEquals("Processing resources have not scaled as expected!",
                SERVER_EXPECTED_PROCESSING_RATE_AFTER_SCALING, scaleUp(MEASUREMENT_OVER_THRESHOLD), 0.0);
        assertEquals("Processing resources have not remained as it was expected!",
                SERVER_EXPECTED_PROCESSING_RATE_BEFORE_SCALING, scaleUp(MEASUREMENT_BELOW_THRESHOLD), 0.0);

        assertEquals("The server was not added!", EXPECTED_NUMBER_OF_SERVERS_AFTER_ADDING,
                addNewServer(MEASUREMENT_OVER_THRESHOLD), 0.0);
        assertEquals("The number of servers is not as expected!", EXPECTED_NUMBER_OF_SERVERS_BEFORE_ADDING,
                addNewServer(MEASUREMENT_BELOW_THRESHOLD), 0.0);

        assertEquals("The server was not added!", EXPECTED_NUMBER_OF_SERVERS_AFTER_ADDING,
                addClonedServer(MEASUREMENT_OVER_THRESHOLD), 0.0);
        assertEquals("The number of servers is not as expected!", EXPECTED_NUMBER_OF_SERVERS_BEFORE_ADDING,
                addClonedServer(MEASUREMENT_BELOW_THRESHOLD), 0.0);
    }

    private int addNewServer(final double m) {
        final PCMResourceSetPartition pcmResourceSet = readPcmModelAndApplyTransformationRules(m,
                TRANSFORMATION_RULES_ADD_SERVER_PATH);

        final Allocation allocation = pcmResourceSet.getAllocation();
        int numOfServer1Client = 0, numOfServer2Client = 0;
        for (final Connector connector : allocation.getSystem_Allocation().getConnectors__ComposedStructure()) {
            if (connector instanceof AssemblyConnector) {

                final AssemblyConnector assemblyConnector = (AssemblyConnector) connector;
                final AssemblyContext assemblyContextProviding = assemblyConnector
                        .getProvidingAssemblyContext_AssemblyConnector();
                final AssemblyContext assemblyContextRequiring = assemblyConnector
                        .getRequiringAssemblyContext_AssemblyConnector();

                if (assemblyContextProviding.getEncapsulatedComponent__AssemblyContext().getEntityName()
                        .equals("server1")
                        && assemblyContextRequiring.getEncapsulatedComponent__AssemblyContext().getEntityName()
                                .equals("client")) {
                    numOfServer1Client++;
                }

                if (assemblyContextProviding.getEncapsulatedComponent__AssemblyContext().getEntityName()
                        .equals("server2")
                        && assemblyContextRequiring.getEncapsulatedComponent__AssemblyContext().getEntityName()
                                .equals("client")) {
                    numOfServer2Client++;
                }
            }
        }
        return numOfServer1Client + numOfServer2Client;
    }

    private int addClonedServer(final double m) {
        final PCMResourceSetPartition pcmResourceSet = readPcmModelAndApplyTransformationRules(m,
                TRANSFORMATION_RULES_ADD_DUPLICATED_SERVER_PATH);

        final Allocation allocation = pcmResourceSet.getAllocation();
        int numOfIServerProviders = 0;
        for (final Connector connector : allocation.getSystem_Allocation().getConnectors__ComposedStructure()) {
            if (connector instanceof AssemblyConnector) {

                final AssemblyConnector assemblyConnector = (AssemblyConnector) connector;
                final AssemblyContext assemblyContextProviding = assemblyConnector
                        .getProvidingAssemblyContext_AssemblyConnector();
                final AssemblyContext assemblyContextRequiring = assemblyConnector
                        .getRequiringAssemblyContext_AssemblyConnector();

                if (assemblyContextProviding.getEncapsulatedComponent__AssemblyContext().getEntityName()
                        .equals("server1")
                        && assemblyContextRequiring.getEncapsulatedComponent__AssemblyContext().getEntityName()
                                .equals("client")) {
                    numOfIServerProviders++;
                }
            }
        }
        return numOfIServerProviders;
    }

    /**
     * Performs scaling up with the measurement passed as parameter. If the measurement is over the
     * threshold the scaling up should be performed, otherwise not.
     *
     * @param m
     *            measurement that defines whether the scaling up is performed or not.
     * @return processing resource of the server that is to be scaled up.
     */
    private double scaleUp(final double m) {
        final PCMResourceSetPartition pcmResourceSet = readPcmModelAndApplyTransformationRules(m,
                TRANSFORMATION_RULES_SCALE_UP_PATH);

        final Allocation allocation = pcmResourceSet.getAllocation();
        final ResourceEnvironment resourceEnvironment = allocation.getTargetResourceEnvironment_Allocation();
        final Iterator<ResourceContainer> iteratorResourceContainer = resourceEnvironment
                .getResourceContainer_ResourceEnvironment().iterator();
        while (iteratorResourceContainer.hasNext()) {
            final ResourceContainer resourceContainer = iteratorResourceContainer.next();
            if (resourceContainer.getEntityName().equals(SERVER_RESOURCE_CONTAINER_NAME)) {
                final Iterator<ProcessingResourceSpecification> iteratorProcessingResourceSpecification = resourceContainer
                        .getActiveResourceSpecifications_ResourceContainer().iterator();
                while (iteratorProcessingResourceSpecification.hasNext()) {
                    final ProcessingResourceSpecification processingResourceSpecification = iteratorProcessingResourceSpecification
                            .next();
                    return Double.parseDouble(processingResourceSpecification
                            .getProcessingRate_ProcessingResourceSpecification().getSpecification());
                }
            }
        }
        assertTrue("The test reached the end!", false);
        return Double.NaN;
    }

    /**
     * Performs outsourcing with the measurement passed as parameter. If the measurement is over the
     * threshold the outsourcing should be performed, otherwise not.
     *
     * @param m
     *            measurement that defines whether the outsourcing is performed or not.
     * @return branch probability that is to be increased.
     */
    private double outsource(final double m) {
        final PCMResourceSetPartition pcmResourceSet = readPcmModelAndApplyTransformationRules(m,
                TRANSFORMATION_RULES_OUTSOURCE_PATH);
        final TreeIterator<EObject> pcmModelIterator = pcmResourceSet.getAllocation().eAllContents();
        /*
         * Iterate over all the elements of the allocation diagram.
         */
        while (pcmModelIterator.hasNext()) {
            final EObject root = pcmModelIterator.next();
            /*
             * We are interested in AllocationContexts only because we can get to the server which
             * contains the SEFF that is of our interest.
             */
            if (root instanceof AllocationContext) {
                final AllocationContext serverAllocationContext = (AllocationContext) root;
                final AssemblyContext serverAssemblyContext = serverAllocationContext
                        .getAssemblyContext_AllocationContext();
                /*
                 * The server that contains our SEFF is of type BasicComponent.
                 */
                if (serverAssemblyContext.getEncapsulatedComponent__AssemblyContext() instanceof BasicComponent) {
                    final BasicComponent serverBasicComponent = (BasicComponent) serverAssemblyContext
                            .getEncapsulatedComponent__AssemblyContext();
                    final EList<ServiceEffectSpecification> serverSeffs = serverBasicComponent
                            .getServiceEffectSpecifications__BasicComponent();
                    /*
                     * We iterate all the SEFFs within the BasicComponent.
                     */
                    for (final ServiceEffectSpecification seff : serverSeffs) {
                        /*
                         * ResourceDemandingSEFF in particular are of our interest.
                         */
                        if (seff instanceof ResourceDemandingSEFF) {
                            final TreeIterator<EObject> seffIterator = seff.eAllContents();
                            /*
                             * We now iterate every ResourceDemandingSEFF in attempt to find
                             * ProbabilisticBranchTransition.
                             */
                            while (seffIterator.hasNext()) {
                                final EObject seffObject = seffIterator.next();
                                /*
                                 * Once we find our ProbabilisticBranchTransitions we compare their
                                 * values with the expected ones.
                                 */
                                if (seffObject instanceof ProbabilisticBranchTransition) {
                                    final ProbabilisticBranchTransition branchTransition = (ProbabilisticBranchTransition) seffObject;
                                    if (branchTransition.getEntityName().equals(BRANCH_2_ENTITY_NAME)) {
                                        return branchTransition.getBranchProbability();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue("The test reached the end!", false);
        return Double.NaN;
    }

    /**
     * Creates a measurement out of parameter "m", reads in the PCM model from the folder
     * "testmodel", performs the QVTo rules that are placed in the folder "testmodel/rules" and
     * returns the resulting PCM model. The PCM model could be changed or not, depending on the
     * parameter "m".
     *
     * @param m
     *            measurement.
     * @return The PCM model after the rules from "testmodel/rules" have been applied.
     */
    private PCMResourceSetPartition readPcmModelAndApplyTransformationRules(final double m,
            final String reconfigurationRulesFolderPath) {
        /*
         * Create a measurement.
         */
        final MeasurementSpecification measurementSpecification = MonitorRepositoryFactory.eINSTANCE
                .createMeasurementSpecification();
        measurementSpecification.setId("_sEx-cMLAEeSZr8oGpigbHA");
        measurementSpecification.setMetricDescription(MetricDescriptionConstants.RESPONSE_TIME_METRIC);
        measurementSpecification.setStatisticalCharacterization(StatisticalCharacterizationEnum.ARITHMETIC_MEAN);
        measurementSpecification.setTemporalRestriction(null);

        final RuntimeMeasurement measurement = RuntimeMeasurementFactory.eINSTANCE.createRuntimeMeasurement();
        measurement.setMeasuringValue(m);
        measurement.setMeasurementSpecification(measurementSpecification);

        final Resource.Factory repositoryFactory = new RepositoryResourceFactoryImpl();
        final Resource.Factory resourceEnvironmentFactory = new ResourceenvironmentResourceFactoryImpl();
        final Resource.Factory systemFactory = new SystemResourceFactoryImpl();
        final Resource.Factory allocationFactory = new AllocationResourceFactoryImpl();
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(REPOSITORY_EXTENSION, repositoryFactory);
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(RESOURCE_ENVIRONMENT_EXTENSION,
                resourceEnvironmentFactory);
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(SYSTEM_EXTENSION, systemFactory);
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(ALLOCATION_EXTENSION, allocationFactory);

        /**
         * This means that the workspace for running the test should be set to the project
         * directory.
         */
        final URI projectAbsoluteUri = URI.createFileURI(EcorePlugin.getWorkspaceRoot().getLocation().toString() + "/");
        EcorePlugin.getPlatformResourceMap().put(PROJECT_NAME, projectAbsoluteUri);

        final URI repositoryURI = EcorePlugin.resolvePlatformResourcePath(REPOSITORY_PATH);
        final URI resourceEnvironmentURI = EcorePlugin.resolvePlatformResourcePath(RESOURCE_ENVIRONMENT_PATH);
        final URI systemURI = EcorePlugin.resolvePlatformResourcePath(SYSTEM_PATH);
        final URI allocationURI = EcorePlugin.resolvePlatformResourcePath(ALLOCATION_PATH);

        /*
         * Read in the PCM model.
         */
        final PCMResourceSetPartition pcmResourceSet = new PCMResourceSetPartition();
        pcmResourceSet.loadModel(repositoryURI);
        pcmResourceSet.loadModel(resourceEnvironmentURI);
        pcmResourceSet.loadModel(systemURI);
        pcmResourceSet.loadModel(allocationURI);
        final TreeIterator<EObject> pcmModelIterator = pcmResourceSet.getRepositories().get(0).eAllContents();
        EObject monitoredElement = null;
        while (pcmModelIterator.hasNext()) {
            final EObject element = pcmModelIterator.next();
            final EAttribute id = element.eClass().getEIDAttribute();
            final Object idAttribute = element.eGet(id);
            if (idAttribute.toString().equals("_1P7G0LwGEeSxGbiYbg6Waw")) {
                monitoredElement = element;
            }
        }

        /*
         * Put the PCM model into the MDSD blackboard.
         */
        final MDSDBlackboard blackboard = new MDSDBlackboard();
        blackboard.addPartition(LoadPCMModelsIntoBlackboardJob.PCM_MODELS_PARTITION_ID, pcmResourceSet);
        final IModelAccess modelAccess = new ModelAccess(blackboard);
        modelAccess.getRuntimeMeasurementModel().getMeasurements().add(measurement);

        /*
         * Create the configuration for the QVTo executor.
         */
        final Map<String, Object> configuration = new HashMap<String, Object>();
        configuration.put(ALLOCATION_FILE_CONFIGURATION_KEY, allocationURI.path());
        configuration.put(PMS_CONFIGURATION_KEY, EcorePlugin.resolvePlatformResourcePath(PMS_MODEL_PATH).path());
        configuration.put(RECONFIGURATION_RULES_CONFIGURATION_KEY,
                EcorePlugin.resolvePlatformResourcePath(reconfigurationRulesFolderPath).path());

        final SimuLizarWorkflowConfiguration swfc = new SimuLizarWorkflowConfiguration(configuration);
        swfc.setMonitorRepositoryFile(configuration.get(PMS_CONFIGURATION_KEY).toString());
        swfc.setReconfigurationRulesFolder(configuration.get(RECONFIGURATION_RULES_CONFIGURATION_KEY).toString());
        final QVTOExecutor qvtoExecutor = new QVTOExecutor(modelAccess, swfc);
        qvtoExecutor.executeRules(monitoredElement);
        return pcmResourceSet;
    }

}