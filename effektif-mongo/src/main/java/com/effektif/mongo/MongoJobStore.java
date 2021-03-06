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
package com.effektif.mongo;

import static com.effektif.mongo.JobFields.*;
import static com.effektif.mongo.MongoDb._ID;
import static com.effektif.mongo.MongoHelper.*;

import java.util.*;

import com.effektif.workflow.impl.WorkflowEngineImpl;
import com.effektif.workflow.impl.job.*;
import com.mongodb.*;
import org.bson.types.ObjectId;

import com.effektif.workflow.api.model.WorkflowInstanceId;
import com.effektif.workflow.impl.configuration.Brewable;
import com.effektif.workflow.impl.configuration.Brewery;
import com.effektif.workflow.impl.util.Time;
import com.effektif.workflow.impl.workflowinstance.LockImpl;


public class MongoJobStore implements JobStore, Brewable {
  
  protected MongoObjectMapper mongoMapper;
  protected String lockOwner;
  protected MongoCollection jobsCollection;
  protected MongoCollection archivedJobsCollection;
  
  @Override
  public void brew(Brewery brewery) {
    MongoDb mongoDb = brewery.get(MongoDb.class);
    MongoConfiguration mongoConfiguration = brewery.get(MongoConfiguration.class);
    this.jobsCollection = mongoDb.createCollection(mongoConfiguration.getJobsCollectionName());
    this.archivedJobsCollection = mongoDb.createCollection(mongoConfiguration.getJobsArchivedCollectionName());
    this.mongoMapper = brewery.get(MongoObjectMapper.class);

    WorkflowEngineImpl workflowEngine = brewery.get(WorkflowEngineImpl.class);
    lockOwner = workflowEngine.getId();
  }

  public void saveJob(Job job) {

    if (job.key == null) {
      job.key = new ObjectId().toString();
    }

    BasicDBObject dbJob = writeJob(job);
    BasicDBObject query = new BasicDBObject(KEY, dbJob.get(KEY));
    jobsCollection.update("upsert-job", query, dbJob, true, false);

  }

  public Iterator<String> getWorkflowInstanceIdsToLockForJobs() {
    DBObject query = buildLockNextJobQuery()
      .push(WORKFLOW_INSTANCE_ID).append("$exists", true).pop()
      .get();
    // TODO use MongoQuery filterOrganization(query, JobFields.organizationId);
    DBObject retrieveFields = new BasicDBObject(WORKFLOW_INSTANCE_ID, true);
    DBCursor jobsDueHavingProcessInstance = jobsCollection.find("jobs-having-process-instance", query, retrieveFields);
    List<String> processInstanceIds = new ArrayList<>();
    while (jobsDueHavingProcessInstance.hasNext()) {
      DBObject partialJob = jobsDueHavingProcessInstance.next();
      Object processInstanceId = partialJob.get(WORKFLOW_INSTANCE_ID);
      processInstanceIds.add(processInstanceId.toString());
    }
    return processInstanceIds.iterator();
  }

  @Override
  public Job lockNextJob() {
    DBObject query = buildLockNextJobQuery()
      .push(WORKFLOW_INSTANCE_ID).append("$exists", false).pop()
      .get();
    // TODO use MongoQuery filterOrganization(query, JobFields.organizationId);
    return lockNextJob(query);
  }

  @Override
  public Job lockJobByKey(String key) {
    BasicDBObject query = new BasicDBObject();
    query.append(KEY, key);
    query.append(LOCK, new BasicDBObject("$exists", false));

    return lockNextJob(query);
  }

  public Job lockNextJob(DBObject query) {
    DBObject dbLock = BasicDBObjectBuilder.start()
      .append(TIME, Time.now().toDate())
      .append(OWNER, lockOwner)
      .get();
    DBObject update = BasicDBObjectBuilder.start()
      .push("$set").append(LOCK, dbLock).pop()
      .get();
    BasicDBObject dbJob = jobsCollection.findAndModify("lock-next-job", query, update);
    if (dbJob!=null) {
      return readJob(dbJob);
    }
    return null;
  }

  protected BasicDBObjectBuilder buildLockNextJobQuery() {
    Date now = Time.now().toDate();
    return BasicDBObjectBuilder.start()
      .append("$or", new DBObject[]{
        new BasicDBObject(DUE_DATE, new BasicDBObject("$exists", false)),
        new BasicDBObject(DUE_DATE, new BasicDBObject("$lte", now))
      })
      .push(LOCK).append("$exists", false).pop()
      .push(DONE).append("$exists", false).pop();
  }

  public Job readJob(BasicDBObject dbJob) {
    List<BasicDBObject> dbExecutions = (List<BasicDBObject>) dbJob.get(EXECUTIONS);
    dbJob.remove(EXECUTIONS);

    Job job = mongoMapper.read(dbJob, Job.class);
    if (dbExecutions != null && dbExecutions.size() > 0) readExecutions(job, dbExecutions);

    return job;
  }

  public void readExecutions(Job job, List<BasicDBObject> dbExecutions) {
    if (dbExecutions!=null && !dbExecutions.isEmpty()) {
      job.executions = new LinkedList<>();
      for (BasicDBObject dbJobExecution: dbExecutions) {
        JobExecution jobExecution = new JobExecution();
        jobExecution.error = readBoolean(dbJobExecution, ERROR);
        jobExecution.logs = readString(dbJobExecution, LOGS);
        jobExecution.time = readTime(dbJobExecution, TIME);
        jobExecution.duration = readLong(dbJobExecution, DURATION);
        job.executions.add(jobExecution);
      }
    }
  }

  public BasicDBObject writeJob(Job job) {
    return mongoMapper.write(job);
  }

  public void writeExecutions(BasicDBObject dbJob, LinkedList<JobExecution> jobExecutions) {
    if (jobExecutions!=null && !jobExecutions.isEmpty()) {
      List<BasicDBObject> dbExecutions = new ArrayList<>();
      for (JobExecution jobExecution: jobExecutions) {
        BasicDBObject dbJobExecution = new BasicDBObject();
        writeBooleanOpt(dbJobExecution, ERROR, jobExecution.error);
        writeStringOpt(dbJobExecution, LOGS, jobExecution.logs);
        writeTimeOpt(dbJobExecution, TIME, jobExecution.time);
        writeLongOpt(dbJobExecution, DURATION, jobExecution.duration);
        dbExecutions.add(dbJobExecution);
      }
      dbJob.put(EXECUTIONS, dbExecutions);
    }
  }

  public void writeLock(BasicDBObject dbJob, LockImpl lock) {
    if (lock!=null) {
      BasicDBObject dbLock = new BasicDBObject();
      writeTimeOpt(dbLock, TIME, lock.time);
      writeStringOpt(dbLock, OWNER, lock.owner);
      dbJob.put(LOCK, dbLock);
    }
  }

  @Override
  public void deleteAllJobs() {
    jobsCollection.remove("delete-all-jobs", new BasicDBObject(), false);
  }

  public List<Job> findAllJobs(JobQuery jobQuery) {
    return findJobs(jobsCollection, jobQuery);
  }


  @Override
  public List<Job> findAllJobs() {
    return findJobs(this.jobsCollection, new JobQuery());
  }

  @Override
  public List<Job> findJobs(JobQuery query) {
    return findJobs(this.jobsCollection, query);
  }

  protected List<Job> findJobs(MongoCollection collection, JobQuery jobQuery) {
    List<Job> jobs = new ArrayList<Job>();
    BasicDBObject query = createDbQuery(jobQuery);
    DBCursor jobCursor = collection.find("find-jobs", query);
    while (jobCursor.hasNext()) {
      BasicDBObject dbJob = (BasicDBObject) jobCursor.next();
      Job job = readJob(dbJob);
      jobs.add(job);
    }
    return jobs;
  }

  public BasicDBObject createDbQuery(JobQuery query) {
    if (query==null) {
      query = new JobQuery();
    }
    BasicDBObject dbQuery = new BasicDBObject();
    // TODO use MongoQuery filterOrganization(dbQuery, JobFields.organizationId);
    if (query.getJobId()!=null) {
      dbQuery.append(_ID, new ObjectId(query.getJobId()));
    }
    if (query.getWorkflowId() != null) {
      dbQuery.append(WORKFLOW_ID, new ObjectId(query.getWorkflowId().getInternal()));
    }
    return dbQuery;
  }

  @Override
  public void deleteJobById(String jobId) {
    BasicDBObject dbQuery = createDbQuery(new JobQuery().jobId(jobId));
    jobsCollection.remove("delete-job", dbQuery);
  }

  @Override
  public void deleteJob(Job job) {
    if (job.id != null) deleteJobById(job.id);
    else {
      if (job.key != null) {
        BasicDBObject dbQuery = new BasicDBObject();
        dbQuery.append(KEY, job.key);
        jobsCollection.remove("delete-job-by-key", dbQuery);
      } else {
        throw new RuntimeException("either id or key of the job to delete should have a value.");
      }
    }
  }

  @Override
  public void deleteJobByScope(WorkflowInstanceId workflowInstanceId, String activityInstanceId) {
    BasicDBObject dbQuery = new Query()
      .equal(WORKFLOW_INSTANCE_ID, new ObjectId(workflowInstanceId.getInternal()))
      .equalOpt(ACTIVITY_INSTANCE_ID, activityInstanceId)
      .get();
    jobsCollection.remove("delete-job", dbQuery);
  }

  @Override
  public void saveArchivedJob(Job job) {
    BasicDBObject dbJob = writeJob(job);
    archivedJobsCollection.save("save-archived-job", dbJob);
  }

  @Override
  public void deleteAllArchivedJobs() {
    archivedJobsCollection.remove("delete-all-archived-jobs", new BasicDBObject(), false);
  }
}
