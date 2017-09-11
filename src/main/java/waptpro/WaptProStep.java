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

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.jenkinsci.plugins.workflow.steps.Step; 
import org.jenkinsci.plugins.workflow.steps.StepContext; 
import org.jenkinsci.plugins.workflow.steps.StepDescriptor; 
import org.jenkinsci.plugins.workflow.steps.StepExecution; 
import hudson.model.TaskListener;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map; 
import java.util.Map.Entry;
import java.util.Set; 

public class WaptProStep extends Step/*AbstractStepImpl*/ {
    
    private final String reportsFolder;
    private final String reportFiles;
    private final boolean checkTestResult;
    
    @DataBoundConstructor
    public WaptProStep(String reportsFolder, String reportFiles, boolean checkTestResult) 
    {
        this.reportsFolder = reportsFolder;
        this.reportFiles = reportFiles;
        this.checkTestResult = checkTestResult;
    }

    public String getReportFiles() {
        return this.reportFiles;
    }

    public boolean getCheckTestResult() {
           return this.checkTestResult;
    }
    
    public String getReportsFolder() {
        return this.reportsFolder;
    }
    
    @Override 
    public StepExecution start(StepContext context) throws Exception { 
        return new WaptProStepExecution(this, context); 
    } 
    
    @Extension
    public static class DescriptorImpl extends StepDescriptor/*AbstractStepDescriptorImpl*/ {
      //  public DescriptorImpl() { super(WaptProStepExecution.class);}

        @Override
        public String getFunctionName() {
            return "waptProReport";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Publish WAPT Pro reports";
        }
        
        @Override 
        public Step newInstance(Map<String,Object> arguments) { 
            String reportsFolder;
            String reportFiles;
            boolean checkTestResult;
            
            if(arguments.size() != 3)
            {
                throw new IllegalArgumentException("Invalid number of arguments"); 
            }
            
            reportsFolder = (String)arguments.get("reportsFolder");
            reportFiles = (String)arguments.get("reportFiles");
            checkTestResult = (Boolean)arguments.get("checkTestResult");
          
            return new WaptProStep(reportsFolder, reportFiles, checkTestResult); 
        }         
        
        @Override 
        public Set<Class<?>> getRequiredContext() { 
            return Collections.<Class<?>>singleton(TaskListener.class); 
        }         
    }
}