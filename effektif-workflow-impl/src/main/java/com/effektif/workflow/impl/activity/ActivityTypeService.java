/*
 * Copyright 2014 Effektif GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.effektif.workflow.impl.activity;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.effektif.workflow.api.Configuration;
import com.effektif.workflow.api.workflow.Activity;
import com.effektif.workflow.api.workflow.Trigger;
import com.effektif.workflow.impl.activity.types.AdapterActivityImpl;
import com.effektif.workflow.impl.activity.types.CallImpl;
import com.effektif.workflow.impl.activity.types.EmailTaskImpl;
import com.effektif.workflow.impl.activity.types.EmbeddedSubprocessImpl;
import com.effektif.workflow.impl.activity.types.EndEventImpl;
import com.effektif.workflow.impl.activity.types.ExclusiveGatewayImpl;
import com.effektif.workflow.impl.activity.types.FormTriggerImpl;
import com.effektif.workflow.impl.activity.types.HttpServiceTaskImpl;
import com.effektif.workflow.impl.activity.types.JavaServiceTaskImpl;
import com.effektif.workflow.impl.activity.types.NoneTaskImpl;
import com.effektif.workflow.impl.activity.types.ParallelGatewayImpl;
import com.effektif.workflow.impl.activity.types.ReceiveTaskImpl;
import com.effektif.workflow.impl.activity.types.ScriptTaskImpl;
import com.effektif.workflow.impl.activity.types.StartEventImpl;
import com.effektif.workflow.impl.activity.types.UserTaskImpl;
import com.effektif.workflow.impl.configuration.Brewable;
import com.effektif.workflow.impl.configuration.Brewery;
import com.effektif.workflow.impl.data.types.ObjectTypeImpl;
import com.effektif.workflow.impl.util.Exceptions;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @author Tom Baeyens
 */
public class ActivityTypeService implements Brewable {
  
  // private static final Logger log = LoggerFactory.getLogger(ActivityTypeService.class);
  
  protected ObjectMapper objectMapper;
  protected Configuration configuration;

  // maps json type names to activity descriptors
  protected Map<String, ActivityDescriptor> activityTypeDescriptors = new LinkedHashMap<>();
  // maps activity api configuration classes to activity type implementation classes
  protected Map<Class<?>, Class<? extends ActivityType>> activityTypeClasses = new HashMap<>();
  protected Map<Class<?>, ActivityType> activityTypes = new LinkedHashMap<>();
  protected Map<Class<?>, ObjectTypeImpl> activityTypeSerializers = new HashMap<>();

  protected Map<Class<?>, Class<? extends AbstractTriggerImpl>> triggerClasses = new HashMap<>();

  public ActivityTypeService() {
  }

  @Override
  public void brew(Brewery brewery) {
    this.objectMapper = brewery.get(ObjectMapper.class);
    this.configuration = brewery.get(Configuration.class);
    initializeActivityTypes();
    initializeTriggerTypes();
  }

  protected void initializeActivityTypes() {
    registerActivityType(new UserTaskImpl());
    registerActivityType(new ParallelGatewayImpl());
    registerActivityType(new ExclusiveGatewayImpl());
    registerActivityType(new StartEventImpl());
    registerActivityType(new EndEventImpl());
    registerActivityType(new EmailTaskImpl());
    registerActivityType(new ScriptTaskImpl());
    registerActivityType(new AdapterActivityImpl());
    registerActivityType(new CallImpl());
    registerActivityType(new EmbeddedSubprocessImpl());
    registerActivityType(new JavaServiceTaskImpl());
    registerActivityType(new HttpServiceTaskImpl());
    registerActivityType(new NoneTaskImpl());
    registerActivityType(new ReceiveTaskImpl());
  }

  protected void initializeTriggerTypes() {
    registerTriggerType(new FormTriggerImpl());
    
  }

  public void registerActivityType(ActivityType activityType) {
    Class<? extends Activity> activityTypeApiClass = activityType.getActivityApiClass();
    ActivityDescriptor descriptor = activityType.getDescriptor();
    activityTypeClasses.put(activityTypeApiClass, activityType.getClass());
    activityTypes.put(activityTypeApiClass, activityType);
    
    JsonTypeName jsonTypeName = activityTypeApiClass.getAnnotation(JsonTypeName.class);
    if (jsonTypeName==null) {
      throw new RuntimeException("Please add @JsonTypeName annotation to "+activityTypeApiClass);
    }
    activityTypeDescriptors.put(jsonTypeName.value(), descriptor);
    
    // log.debug("Registering "+activityTypeApiClass);
    objectMapper.registerSubtypes(activityTypeApiClass);
  }
  
  public ActivityDescriptor getActivityDescriptor(String jsonTypeName) {
    return activityTypeDescriptors.get(jsonTypeName);
  }

  public void registerTriggerType(AbstractTriggerImpl trigger) {
    Class triggerApiClass = trigger.getTriggerApiClass();
    triggerClasses.put(triggerApiClass, trigger.getClass());
    objectMapper.registerSubtypes(triggerApiClass);
  }

  public ActivityType instantiateActivityType(Activity activityApi) {
    Exceptions.checkNotNullParameter(activityApi, "activityApi");
    Class<? extends ActivityType> activityTypeClass = activityTypeClasses.get(activityApi.getClass());
    if (activityTypeClass==null) {
      throw new RuntimeException("No ActivityType defined for "+activityApi.getClass().getName());
    }
    try {
      return activityTypeClass.newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Couldn't instantiate "+activityTypeClass+": "+e.getMessage(), e);
    }
  }

  public AbstractTriggerImpl instantiateTriggerType(Trigger triggerApi) {
    Exceptions.checkNotNullParameter(triggerApi, "triggerApi");
    Class<? extends AbstractTriggerImpl> triggerTypeClass = triggerClasses.get(triggerApi.getClass());
    if (triggerTypeClass==null) {
      throw new RuntimeException("No trigger type defined for "+triggerApi.getClass().getName());
    }
    try {
      return triggerTypeClass.newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Couldn't instantiate "+triggerTypeClass+": "+e.getMessage(), e);
    }
  }

  public Collection<ActivityType> getActivityTypes() {
    return activityTypes.values();
  }

  public ActivityType<Activity> getActivityType(Class<? extends Activity> activityType) {
    return activityTypes.get(activityType);
  }
}
