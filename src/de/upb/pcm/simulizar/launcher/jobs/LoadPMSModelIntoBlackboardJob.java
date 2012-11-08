package de.upb.pcm.simulizar.launcher.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;

import de.uka.ipd.sdq.codegen.simucontroller.runconfig.SimuComWorkflowConfiguration;
import de.uka.ipd.sdq.workflow.IBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.IJob;
import de.uka.ipd.sdq.workflow.exceptions.JobFailedException;
import de.uka.ipd.sdq.workflow.exceptions.RollbackFailedException;
import de.uka.ipd.sdq.workflow.exceptions.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import de.uka.ipd.sdq.workflow.pcm.blackboard.PCMResourceSetPartition;
import de.uka.ipd.sdq.workflow.pcm.jobs.LoadPCMModelsIntoBlackboardJob;
import de.upb.pcm.simulizar.exceptions.PMSModelLoadException;
import de.upb.pcm.simulizar.launcher.SimulizarConstants;
import de.upb.pcm.simulizar.launcher.partitions.PMSResourceSetPartition;

/**
 * Job for loading pms model into blackboard. Resolving proxies to pcm.
 * 
 * @author Joachim Meyer
 * 
 */
public class LoadPMSModelIntoBlackboardJob implements IJob, IBlackboardInteractingJob<MDSDBlackboard> {

    public static final String PMS_MODEL_PARTITION_ID = "de.upb.pcm.pms";

    private MDSDBlackboard blackboard;

    private final String path;

    /**
     * Constructor
     * 
     * @param configuration
     *            the SimuCom workflow configuration.
     */
    public LoadPMSModelIntoBlackboardJob(final SimuComWorkflowConfiguration configuration) {
        this.path = (String) configuration.getAttributes().get(SimulizarConstants.PMS_FILE);
    }

    /**
     * @see de.uka.ipd.sdq.workflow.IJob#execute(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void execute(final IProgressMonitor monitor) throws JobFailedException, UserCanceledException {
        if (this.getPCMResourceSetPartition() == null) {
            throw new PMSModelLoadException("The PCM models must be loaded first");
        }
       
        final PMSResourceSetPartition prmPartition = new PMSResourceSetPartition(this.getPCMResourceSetPartition());
        if (!this.getPath().equals("")) {
        	
        	//add file protocol if necessary
        	String filePath = getPath();
        	if (!getPath().startsWith("platform:")) {
        		filePath = "file:///" + filePath;
        	}
        	
            prmPartition.loadModel(URI.createURI(filePath));

        }
        this.getBlackboard().addPartition(PMS_MODEL_PARTITION_ID, prmPartition);
        // now resolve all cross references from current resource to PCM
        prmPartition.resolveAllProxiesToPCM();

    }

    /**
     * @return returns the blackboard.
     */
    private MDSDBlackboard getBlackboard() {
        return this.blackboard;
    }

    /**
     * @see de.uka.ipd.sdq.workflow.IJob#getName()
     */
    @Override
    public String getName() {
        return "Perform PMS Model Load";
    }

    /**
     * @return returns the path.
     */
    private String getPath() {
        return this.path;
    }

    /**
     * @return the pcm resource set partition
     */
    private PCMResourceSetPartition getPCMResourceSetPartition() {
        return (PCMResourceSetPartition) (this.getBlackboard()
                .getPartition(LoadPCMModelsIntoBlackboardJob.PCM_MODELS_PARTITION_ID));
    }

    /**
     * @see de.uka.ipd.sdq.workflow.IJob#rollback(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void rollback(final IProgressMonitor monitor) throws RollbackFailedException {

    }

    /**
     * @see de.uka.ipd.sdq.workflow.IBlackboardInteractingJob#setBlackboard(de.uka.ipd.sdq.workflow.Blackboard)
     */
    @Override
    public void setBlackboard(final MDSDBlackboard blackboard) {
        this.blackboard = blackboard;

    }

}