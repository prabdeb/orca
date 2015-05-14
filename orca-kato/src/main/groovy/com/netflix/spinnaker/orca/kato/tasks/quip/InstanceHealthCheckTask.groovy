package com.netflix.spinnaker.orca.kato.tasks.quip

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.orca.DefaultTaskResult
import com.netflix.spinnaker.orca.ExecutionStatus
import com.netflix.spinnaker.orca.RetryableTask
import com.netflix.spinnaker.orca.TaskResult
import com.netflix.spinnaker.orca.pipeline.model.Stage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import retrofit.RetrofitError

@Component
class InstanceHealthCheckTask extends AbstractQuipTask implements RetryableTask  {
  @Autowired ObjectMapper objectMapper

  long backoffPeriod = 10000
  long timeout = 3600000 // 60min

  @Override
  TaskResult execute(Stage stage) {
    Map stageOutputs = [:]
    def instances = stage.context?.instances
    ExecutionStatus executionStatus = ExecutionStatus.SUCCEEDED
    // verify instance list, package, and version are in the context
    if(instances) {
      // trigger patch on target server
      instances.each { String key, Map valueMap ->
        if(!valueMap.healthCheckUrl || valueMap.healthCheckUrl.isEmpty()) {
          throw new RuntimeException("instances map is missing the  healthCheckUrl : ${instances}")
        }

        URL healthCheckUrl = new URL(valueMap.healthCheckUrl)
        def instanceService = createInstanceService("http://${healthCheckUrl.host}:${healthCheckUrl.port}")
        try { // keep trying until we get a 200 or time out
          instanceService.healthCheck(healthCheckUrl.path.substring(1))
        } catch(RetrofitError e) {
          executionStatus = ExecutionStatus.RUNNING
        }
      }
    } else {
      throw new RuntimeException("one or more required parameters are missing : instances (${instances})")
    }
    return new DefaultTaskResult(executionStatus, stageOutputs)
  }
}