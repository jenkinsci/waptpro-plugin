//   Copyright 2016 Softlogica inc.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package waptpro;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;

public class WaptProStepExecution extends StepExecution {

    private transient WaptProStep waptProStep;
    
    @StepContextParameter
    private transient TaskListener listener;

    @StepContextParameter
    private transient FilePath ws;

    @StepContextParameter
    private transient Run build;

    @StepContextParameter
    private transient Launcher launcher;
    
    public WaptProStepExecution(WaptProStep waptProStep, StepContext context)
    {     
        super(context);
        this.waptProStep = waptProStep;    
    }
    
    @Override 
    public boolean start() throws Exception { 
        StepContext cps = getContext();
        this.listener = cps.get(TaskListener.class);
        this.ws = cps.get(FilePath.class);
        this.build = cps.get(Run.class);
        this.launcher = cps.get(Launcher.class);
        
        listener.getLogger().println("Running WAPT Pro step.");
                            
        WaptPro publisher = new WaptPro(this.waptProStep.getReportsFolder(), this.waptProStep.getReportFiles(), this.waptProStep.getCheckTestResult());
        publisher.perform(build, ws, launcher, listener);
        
        cps.onSuccess(null);
        
        return true; 
    }
     
    @Override 
    public void stop(Throwable cause) throws Exception { 

    }     
    /*
    @Override
    protected Void run() throws Exception {
        listener.getLogger().println("Running WAPT Pro step.");
        
        WaptPro publisher = new WaptPro(this.waptProStep.getReportsFolder(), this.waptProStep.getReportFiles(), this.waptProStep.getCheckTestResult());
        publisher.perform(build, ws, launcher, listener);
        
        return null;
    }
    */
     private static final long serialVersionUID = 1L;
}