import { randomUUID } from "node:crypto";

import {
  Prisma,
  DiscussionMessageType,
  MilestoneStatus,
  ProjectStatus,
  TaskPriority,
  type DiscussionCategory,
  type DiscussionMessage,
  type DiscussionRoom,
  type FileMeta,
  type Label,
  type PrismaClient,
  type Project,
  type ProjectMilestone,
  type ProjectNote,
  type ProjectUserState,
  type Task,
  type TaskCategory,
  type TaskNote,
  type TaskStage,
  type TimeLog
} from "@prisma/client";

import { HttpError } from "../common/errors.js";
import type { ClientMeta, ClientClient } from "../lib/client-client.js";
import type { EmployeeMeta, EmployeeClient } from "../lib/employee-client.js";
import { MediaStorageService } from "./media-storage.service.js";

export interface ProjectPayload {
  shortCode?: string;
  name?: string;
  projectName?: string;
  startDate?: string | null;
  deadline?: string | null;
  noDeadline?: boolean | string | null;
  category?: string | null;
  projectCategory?: string | null;
  department?: string | null;
  departmentId?: string | null;
  clientId?: string | null;
  summary?: string | null;
  projectSummary?: string | null;
  tasksNeedAdminApproval?: boolean | string | null;
  currency?: string | null;
  budget?: number | string | null;
  projectBudget?: number | string | null;
  hoursEstimate?: number | string | null;
  allowManualTimeLogs?: boolean | string | null;
  assignedEmployeeIds?: string[] | string | null;
  projectStatus?: string | null;
  progressPercent?: number | string | null;
  calculateProgressThroughTasks?: boolean | string | null;
}

export interface MilestonePayload {
  title?: string;
  milestoneCost?: number | string | null;
  status?: string | null;
  summary?: string | null;
  startDate?: string | null;
  endDate?: string | null;
}

export interface NotePayload {
  title?: string;
  content?: string;
  isPublic?: boolean | null;
}

export interface TaskPayload {
  projectId?: number | string | null;
  title?: string;
  category?: string | number | null;
  startDate?: string | null;
  dueDate?: string | null;
  noDueDate?: boolean | string | null;
  taskStageId?: number | string | null;
  assignedEmployeeIds?: string[] | string | null;
  description?: string | null;
  labelIds?: Array<string | number> | string | null;
  milestoneId?: number | string | null;
  priority?: string | null;
  isPrivate?: boolean | string | null;
  timeEstimate?: boolean | string | null;
  timeEstimateMinutes?: number | string | null;
  isDependent?: boolean | string | null;
  dependentTaskId?: number | string | null;
}

export interface SubtaskPayload {
  taskId?: number | string | null;
  title?: string;
  description?: string | null;
  isDone?: boolean | null;
}

export interface TimeLogPayload {
  projectId?: number | string | null;
  taskId?: number | string | null;
  employeeId?: string | null;
  startDate?: string | null;
  startTime?: string | null;
  endDate?: string | null;
  endTime?: string | null;
  memo?: string | null;
  durationHours?: number | null;
}

export interface WeeklyTimeLogPayload {
  projectId?: number | string | null;
  taskId?: number | string | null;
  employeeId?: string | null;
  days?: Array<{
    date?: string | null;
    hours?: number | string | null;
    memo?: string | null;
  }>;
}

export interface DiscussionCategoryPayload {
  categoryName?: string;
  colorCode?: string | null;
}

export interface DiscussionRoomPayload {
  title?: string;
  categoryId?: number | string | null;
  initialMessage?: string | null;
}

export interface DiscussionMessagePayload {
  content?: string | null;
  parentMessageId?: number | string | null;
}

interface UploadFileInput {
  filename: string | null;
  contentType: string | null;
  data: Buffer;
}

export class ProjectService {
  constructor(
    private readonly prisma: PrismaClient,
    private readonly employeeClient: EmployeeClient,
    private readonly clientClient: ClientClient,
    private readonly mediaStorageService: MediaStorageService
  ) {}

  async createProject(payload: ProjectPayload, actor: string, companyFile?: UploadFileInput | null) {
    const shortCode = normalizeRequired(payload.shortCode, "shortCode is required");
    const name = normalizeRequired(payload.projectName ?? payload.name, "projectName is required");
    const assignedEmployeeIds = normalizeEmployeeIds(payload.assignedEmployeeIds);

    await this.ensureEmployeesExist(assignedEmployeeIds);

    const project = await this.prisma.project.create({
      data: {
        shortCode,
        name,
        startDate: parseDate(payload.startDate),
        deadline: parseDate(payload.deadline),
        noDeadline: normalizeBoolean(payload.noDeadline),
        category: normalizeNullable(payload.projectCategory ?? payload.category),
        department: normalizeNullable(payload.departmentId ?? payload.department),
        clientId: normalizeNullable(payload.clientId),
        summary: normalizeNullable(payload.projectSummary ?? payload.summary),
        tasksNeedAdminApproval: normalizeBoolean(payload.tasksNeedAdminApproval),
        currency: normalizeNullable(payload.currency),
        budget: toDecimal(payload.projectBudget ?? payload.budget),
        hoursEstimate: toInt(payload.hoursEstimate),
        allowManualTimeLogs: normalizeBoolean(payload.allowManualTimeLogs),
        addedBy: actor,
        assignedEmployeeIds,
        projectStatus: normalizeProjectStatus(payload.projectStatus),
        progressPercent: clampPercent(payload.progressPercent),
        calculateProgressThroughTasks: normalizeBoolean(payload.calculateProgressThroughTasks),
        createdBy: actor,
        updatedBy: actor
      }
    });

    if (companyFile) {
      await this.saveProjectFile(project.id, companyFile, actor, "company-files");
    }

    await this.logActivity(project.id, "PROJECT_CREATED", `Project ${project.name} created`, actor);
    return this.getProject(project.id, actor);
  }

  async updateProject(projectId: number, payload: ProjectPayload, actor: string, companyFile?: UploadFileInput | null) {
    await this.ensureProject(projectId);

    const assignedEmployeeIds =
      payload.assignedEmployeeIds !== undefined
        ? normalizeEmployeeIds(payload.assignedEmployeeIds)
        : undefined;

    if (assignedEmployeeIds) {
      await this.ensureEmployeesExist(assignedEmployeeIds);
    }

    const updated = await this.prisma.project.update({
      where: { id: projectId },
      data: {
        name: normalizeOptional(payload.projectName ?? payload.name),
        startDate: optionalDate(payload.startDate),
        deadline: optionalDate(payload.deadline),
        noDeadline: optionalBoolean(payload.noDeadline),
        category: normalizeOptional(payload.projectCategory ?? payload.category),
        department: normalizeOptional(payload.departmentId ?? payload.department),
        clientId: normalizeOptional(payload.clientId),
        summary: normalizeOptional(payload.projectSummary ?? payload.summary),
        tasksNeedAdminApproval: optionalBoolean(payload.tasksNeedAdminApproval),
        currency: normalizeOptional(payload.currency),
        budget: optionalDecimal(payload.projectBudget ?? payload.budget),
        hoursEstimate: optionalInt(payload.hoursEstimate),
        allowManualTimeLogs: optionalBoolean(payload.allowManualTimeLogs),
        assignedEmployeeIds,
        projectStatus: payload.projectStatus ? normalizeProjectStatus(payload.projectStatus) : undefined,
        progressPercent: payload.progressPercent !== undefined ? clampPercent(payload.progressPercent) : undefined,
        calculateProgressThroughTasks: optionalBoolean(payload.calculateProgressThroughTasks),
        updatedBy: actor
      }
    });

    if (companyFile) {
      await this.saveProjectFile(projectId, companyFile, actor, "company-files");
    }

    await this.logActivity(projectId, "PROJECT_UPDATED", `Project ${updated.name} updated`, actor);
    return this.getProject(projectId, actor);
  }

  async deleteProject(projectId: number, actor: string) {
    const project = await this.ensureProject(projectId);
    await this.prisma.project.delete({ where: { id: projectId } });
    await this.logActivity(projectId, "PROJECT_DELETED", `Project ${project.name} deleted`, actor);
  }

  async getProject(projectId: number, requesterId: string) {
    const project = await this.ensureProject(projectId);
    return this.enrichProject(project, requesterId);
  }

  async getProjectWithMetrics(projectId: number, requesterId: string) {
    const project = await this.ensureProject(projectId);
    return this.enrichProject(project, requesterId, true);
  }

  async getAll(requesterId: string) {
    const projects = await this.prisma.project.findMany({
      orderBy: { createdAt: "desc" }
    });
    return Promise.all(projects.map((project) => this.enrichProject(project, requesterId)));
  }

  async listProjectsForEmployee(employeeId: string, requesterId: string) {
    const projects = await this.prisma.project.findMany({
      where: {
        OR: [{ assignedEmployeeIds: { has: employeeId } }, { projectAdminId: employeeId }]
      },
      orderBy: { createdAt: "desc" }
    });
    return Promise.all(projects.map((project) => this.enrichProject(project, requesterId)));
  }

  async addAssignedEmployees(projectId: number, employeeIds: string[], actor: string) {
    const project = await this.ensureProject(projectId);
    const normalized = normalizeEmployeeIds(employeeIds);
    await this.ensureEmployeesExist(normalized);
    const assignedEmployeeIds = [...new Set([...project.assignedEmployeeIds, ...normalized])];

    await this.prisma.project.update({
      where: { id: projectId },
      data: {
        assignedEmployeeIds,
        updatedBy: actor
      }
    });

    await this.logActivity(projectId, "PROJECT_ASSIGNMENT_ADDED", `Assigned employees updated`, actor, {
      employeeIds: normalized
    });
  }

  async removeAssignedEmployee(projectId: number, employeeId: string, actor: string) {
    const project = await this.ensureProject(projectId);
    const assignedEmployeeIds = project.assignedEmployeeIds.filter((id) => id !== employeeId);

    await this.prisma.project.update({
      where: { id: projectId },
      data: {
        assignedEmployeeIds,
        updatedBy: actor
      }
    });

    await this.logActivity(projectId, "PROJECT_ASSIGNMENT_REMOVED", `Removed employee ${employeeId}`, actor);
  }

  async assignProjectAdmin(projectId: number, userId: string, actor: string) {
    await this.employeeClient.ensureEmployeeExists(userId);
    const project = await this.ensureProject(projectId);
    const assigned = project.assignedEmployeeIds.includes(userId)
      ? project.assignedEmployeeIds
      : [...project.assignedEmployeeIds, userId];

    await this.prisma.project.update({
      where: { id: projectId },
      data: {
        projectAdminId: userId,
        assignedEmployeeIds: assigned,
        updatedBy: actor
      }
    });

    await this.logActivity(projectId, "PROJECT_ADMIN_ASSIGNED", `Assigned ${userId} as project admin`, actor);
  }

  async removeProjectAdmin(projectId: number, actor: string) {
    await this.prisma.project.update({
      where: { id: projectId },
      data: {
        projectAdminId: null,
        updatedBy: actor
      }
    });

    await this.logActivity(projectId, "PROJECT_ADMIN_REMOVED", "Removed project admin", actor);
  }

  async updateStatus(projectId: number, status: string, actor: string) {
    await this.prisma.project.update({
      where: { id: projectId },
      data: {
        projectStatus: normalizeProjectStatus(status),
        updatedBy: actor
      }
    });

    await this.logActivity(projectId, "PROJECT_STATUS_UPDATED", `Project status changed to ${status}`, actor);
  }

  async updateProgress(projectId: number, percent: number | string, actor: string) {
    await this.prisma.project.update({
      where: { id: projectId },
      data: {
        progressPercent: clampPercent(percent),
        updatedBy: actor
      }
    });

    await this.logActivity(projectId, "PROJECT_PROGRESS_UPDATED", `Project progress changed`, actor, {
      percent: clampPercent(percent)
    });
  }

  async getProjectCounts() {
    const today = startOfDay(new Date());
    const pendingCount = await this.prisma.project.count({
      where: {
        projectStatus: { notIn: [ProjectStatus.FINISHED, ProjectStatus.CANCELLED] }
      }
    });
    const overdueCount = await this.prisma.project.count({
      where: {
        deadline: { lt: today },
        noDeadline: false,
        projectStatus: { notIn: [ProjectStatus.FINISHED, ProjectStatus.CANCELLED] }
      }
    });

    return {
      pendingCount,
      overdueCount
    };
  }

  async getProjectCountsForEmployee(employeeId: string) {
    const [pendingCount, overdueCount] = await Promise.all([
      this.prisma.project.count({
        where: {
          OR: [{ assignedEmployeeIds: { has: employeeId } }, { projectAdminId: employeeId }],
          projectStatus: { notIn: [ProjectStatus.FINISHED, ProjectStatus.CANCELLED] }
        }
      }),
      this.prisma.project.count({
        where: {
          OR: [{ assignedEmployeeIds: { has: employeeId } }, { projectAdminId: employeeId }],
          deadline: { lt: startOfDay(new Date()) },
          noDeadline: false,
          projectStatus: { notIn: [ProjectStatus.FINISHED, ProjectStatus.CANCELLED] }
        }
      })
    ]);

    return {
      pendingCount,
      overdueCount
    };
  }

  async getProjectCountForEmployee(employeeId: string) {
    const projectCount = await this.prisma.project.count({
      where: {
        OR: [{ assignedEmployeeIds: { has: employeeId } }, { projectAdminId: employeeId }]
      }
    });

    return {
      employeeId,
      projectCount
    };
  }

  async getClientProjectStats(clientId: string) {
    const stats = await this.prisma.project.aggregate({
      where: { clientId },
      _count: { id: true },
      _sum: { budget: true }
    });

    return {
      projectCount: stats._count.id,
      totalEarning: decimalToNumber(stats._sum.budget)
    };
  }

  async listProjectsByClient(clientId: string, requesterId: string) {
    const projects = await this.prisma.project.findMany({
      where: { clientId },
      orderBy: { createdAt: "desc" }
    });
    return Promise.all(projects.map((project) => this.enrichProject(project, requesterId)));
  }

  async importProjectsFromCsv(file: UploadFileInput | null | undefined, actor: string) {
    if (!file || !file.data.length) {
      throw new HttpError(400, "CSV file is required");
    }

    const rows = file.data
      .toString("utf8")
      .split(/\r?\n/)
      .filter((row) => row.trim().length > 0);

    if (rows.length < 2) {
      return [];
    }

    const headers = rows[0].split(",").map((value) => value.trim().toLowerCase());
    const results: Array<{ row: number; status: "CREATED" | "SKIPPED" | "ERROR"; message: string }> = [];

    for (const [index, row] of rows.slice(1).entries()) {
      const values = row.split(",").map((value) => value.trim());
      const record = Object.fromEntries(headers.map((header, headerIndex) => [header, values[headerIndex] ?? ""]));
      const shortCode = record.shortcode || record.short_code || "";
      const projectName = record.projectname || record.name || "";

      if (!shortCode || !projectName) {
        results.push({ row: index + 2, status: "ERROR", message: "shortCode and projectName are required" });
        continue;
      }

      const existing = await this.prisma.project.findUnique({ where: { shortCode } });
      if (existing) {
        results.push({ row: index + 2, status: "SKIPPED", message: "Project shortCode already exists" });
        continue;
      }

      await this.prisma.project.create({
        data: {
          shortCode,
          name: projectName,
          category: normalizeNullable(record.category || record.projectcategory),
          department: normalizeNullable(record.department),
          clientId: normalizeNullable(record.clientid),
          summary: normalizeNullable(record.summary || record.projectsummary),
          currency: normalizeNullable(record.currency),
          budget: toDecimal(record.budget || record.projectbudget),
          hoursEstimate: toInt(record.hoursestimate),
          addedBy: actor,
          assignedEmployeeIds: normalizeEmployeeIds(record.assignedemployeeids),
          projectStatus: normalizeProjectStatus(record.projectstatus),
          noDeadline: normalizeBoolean(record.nodeadline),
          allowManualTimeLogs: normalizeBoolean(record.allowmanualtimelogs),
          tasksNeedAdminApproval: normalizeBoolean(record.tasksneedadminapproval),
          progressPercent: clampPercent(record.progresspercent),
          createdBy: actor,
          updatedBy: actor
        }
      });

      results.push({ row: index + 2, status: "CREATED", message: "Project imported" });
    }

    return results;
  }

  async createProjectCategory(name: string) {
    const normalizedName = normalizeRequired(name, "Category name is required");
    return this.prisma.projectCategory.upsert({
      where: { name: normalizedName },
      update: {},
      create: { name: normalizedName }
    });
  }

  async listProjectCategories() {
    return this.prisma.projectCategory.findMany({ orderBy: { name: "asc" } });
  }

  async deleteProjectCategory(id: number) {
    await this.prisma.projectCategory.delete({ where: { id } });
  }

  async pinProject(projectId: number, employeeId: string) {
    await this.ensureProject(projectId);
    await this.prisma.projectUserState.upsert({
      where: { projectId_employeeId: { projectId, employeeId } },
      update: { pinned: true, pinnedAt: new Date() },
      create: { projectId, employeeId, pinned: true, pinnedAt: new Date() }
    });
  }

  async unpinProject(projectId: number, employeeId: string) {
    await this.ensureProject(projectId);
    await this.prisma.projectUserState.upsert({
      where: { projectId_employeeId: { projectId, employeeId } },
      update: { pinned: false, pinnedAt: null },
      create: { projectId, employeeId, pinned: false }
    });
  }

  async archiveProject(projectId: number, employeeId: string) {
    await this.ensureProject(projectId);
    await this.prisma.projectUserState.upsert({
      where: { projectId_employeeId: { projectId, employeeId } },
      update: { archived: true, archivedAt: new Date() },
      create: { projectId, employeeId, archived: true, archivedAt: new Date() }
    });
  }

  async unarchiveProject(projectId: number, employeeId: string) {
    await this.ensureProject(projectId);
    await this.prisma.projectUserState.upsert({
      where: { projectId_employeeId: { projectId, employeeId } },
      update: { archived: false, archivedAt: null },
      create: { projectId, employeeId, archived: false }
    });
  }

  async listPinnedProjects(employeeId: string) {
    return this.prisma.projectUserState.findMany({
      where: { employeeId, pinned: true },
      orderBy: { pinnedAt: "desc" }
    });
  }

  async listArchivedProjects(employeeId: string) {
    return this.prisma.projectUserState.findMany({
      where: { employeeId, archived: true },
      orderBy: { archivedAt: "desc" }
    });
  }

  async listPinnedProjectDetails(employeeId: string) {
    const states = await this.listPinnedProjects(employeeId);
    const projects = await Promise.all(states.map((state) => this.enrichProjectById(state.projectId, employeeId)));
    return projects.filter(Boolean);
  }

  async listArchivedProjectDetails(employeeId: string) {
    const states = await this.listArchivedProjects(employeeId);
    const projects = await Promise.all(states.map((state) => this.enrichProjectById(state.projectId, employeeId)));
    return projects.filter(Boolean);
  }

  async listProjectActivity(projectId: number) {
    await this.ensureProject(projectId);
    return this.prisma.projectActivity.findMany({
      where: { projectId },
      orderBy: { createdAt: "desc" }
    });
  }

  async createMilestone(projectId: number, payload: MilestonePayload, actor: string) {
    await this.ensureProject(projectId);
    const milestone = await this.prisma.projectMilestone.create({
      data: {
        projectId,
        title: normalizeRequired(payload.title, "Milestone title is required"),
        milestoneCost: toDecimal(payload.milestoneCost),
        status: normalizeMilestoneStatus(payload.status),
        summary: normalizeNullable(payload.summary),
        startDate: parseDate(payload.startDate),
        endDate: parseDate(payload.endDate),
        createdBy: actor,
        updatedBy: actor
      }
    });

    await this.logActivity(projectId, "MILESTONE_CREATED", `Milestone ${milestone.title} created`, actor);
    return this.serializeMilestone(milestone);
  }

  async listMilestones(projectId: number) {
    await this.ensureProject(projectId);
    const milestones = await this.prisma.projectMilestone.findMany({
      where: { projectId },
      orderBy: { createdAt: "desc" }
    });
    return milestones.map((milestone) => this.serializeMilestone(milestone));
  }

  async getMilestone(projectId: number, milestoneId: number) {
    await this.ensureProject(projectId);
    const milestone = await this.ensureMilestone(projectId, milestoneId);
    return this.serializeMilestone(milestone);
  }

  async updateMilestone(projectId: number, milestoneId: number, payload: MilestonePayload, actor: string) {
    await this.ensureProject(projectId);
    const milestone = await this.ensureMilestone(projectId, milestoneId);
    const updated = await this.prisma.projectMilestone.update({
      where: { id: milestone.id },
      data: {
        title: normalizeOptional(payload.title),
        milestoneCost: optionalDecimal(payload.milestoneCost),
        status: payload.status ? normalizeMilestoneStatus(payload.status) : undefined,
        summary: normalizeOptional(payload.summary),
        startDate: optionalDate(payload.startDate),
        endDate: optionalDate(payload.endDate),
        updatedBy: actor
      }
    });

    await this.logActivity(projectId, "MILESTONE_UPDATED", `Milestone ${updated.title} updated`, actor);
    return this.serializeMilestone(updated);
  }

  async deleteMilestone(projectId: number, milestoneId: number, actor: string) {
    await this.ensureProject(projectId);
    const milestone = await this.ensureMilestone(projectId, milestoneId);
    await this.prisma.projectMilestone.delete({ where: { id: milestone.id } });
    await this.logActivity(projectId, "MILESTONE_DELETED", `Milestone ${milestone.title} deleted`, actor);
  }

  async updateMilestoneStatus(projectId: number, milestoneId: number, status: string, actor: string) {
    await this.ensureProject(projectId);
    const milestone = await this.ensureMilestone(projectId, milestoneId);
    const updated = await this.prisma.projectMilestone.update({
      where: { id: milestone.id },
      data: {
        status: normalizeMilestoneStatus(status),
        updatedBy: actor
      }
    });

    await this.logActivity(projectId, "MILESTONE_STATUS_UPDATED", `Milestone ${updated.title} status updated`, actor);
    return this.serializeMilestone(updated);
  }

  async uploadProjectFile(projectId: number, file: UploadFileInput, actor: string) {
    await this.ensureProject(projectId);
    return this.saveProjectFile(projectId, file, actor, "project-files");
  }

  async uploadTaskFile(taskId: number, file: UploadFileInput, actor: string) {
    const task = await this.ensureTask(taskId);
    const uploaded = await this.mediaStorageService.saveUploadedFile(file, `projects/${task.projectId}/tasks/${taskId}`);

    if (!uploaded) {
      throw new HttpError(400, "File is required");
    }

    const fileMeta = await this.prisma.fileMeta.create({
      data: {
        projectId: task.projectId,
        taskId,
        filename: file.filename ?? "attachment",
        bucket: "cloudinary",
        path: uploaded.objectKey ?? uploaded.url,
        url: uploaded.url,
        mimeType: file.contentType,
        size: file.data.length,
        objectKey: uploaded.objectKey,
        uploadedBy: actor
      }
    });

    await this.logActivity(task.projectId, "TASK_FILE_UPLOADED", `Uploaded task file ${fileMeta.filename}`, actor, {
      taskId
    });
    return this.serializeFile(fileMeta);
  }

  async listProjectFiles(projectId: number) {
    await this.ensureProject(projectId);
    const files = await this.prisma.fileMeta.findMany({
      where: {
        projectId,
        taskId: null,
        milestoneId: null
      },
      orderBy: { createdAt: "desc" }
    });
    return files.map((file) => this.serializeFile(file));
  }

  async listTaskFiles(taskId: number) {
    await this.ensureTask(taskId);
    const files = await this.prisma.fileMeta.findMany({
      where: { taskId },
      orderBy: { createdAt: "desc" }
    });
    return files.map((file) => this.serializeFile(file));
  }

  async getFileDownload(fileId: number) {
    const file = await this.prisma.fileMeta.findUnique({ where: { id: fileId } });
    if (!file) {
      throw new HttpError(404, "File not found");
    }

    return file;
  }

  async deleteFile(fileId: number, actor: string) {
    const file = await this.prisma.fileMeta.findUnique({ where: { id: fileId } });
    if (!file) {
      throw new HttpError(404, "File not found");
    }

    await this.mediaStorageService.deleteUploadedFile(file.objectKey);
    await this.prisma.fileMeta.delete({ where: { id: fileId } });

    if (file.projectId) {
      await this.logActivity(file.projectId, "FILE_DELETED", `Deleted file ${file.filename}`, actor);
    }
  }

  async createProjectNote(projectId: number, payload: NotePayload, actor: string) {
    await this.ensureProject(projectId);
    const note = await this.prisma.projectNote.create({
      data: {
        projectId,
        title: normalizeNullable(payload.title),
        content: normalizeNullable(payload.content),
        isPublic: payload.isPublic ?? true,
        ownerEmployeeId: actor,
        createdBy: actor
      }
    });
    await this.logActivity(projectId, "PROJECT_NOTE_CREATED", `Added project note`, actor);
    return this.serializeNote(note);
  }

  async listProjectNotes(projectId: number, requesterId: string) {
    await this.ensureProject(projectId);
    const notes = await this.prisma.projectNote.findMany({
      where: {
        projectId,
        OR: [{ isPublic: true }, { ownerEmployeeId: requesterId }]
      },
      orderBy: { createdAt: "desc" }
    });
    return notes.map((note) => this.serializeNote(note));
  }

  async updateProjectNote(noteId: number, payload: NotePayload, actor: string) {
    const note = await this.prisma.projectNote.findUnique({ where: { id: noteId } });
    if (!note) {
      throw new HttpError(404, "Project note not found");
    }

    const updated = await this.prisma.projectNote.update({
      where: { id: noteId },
      data: {
        title: normalizeOptional(payload.title),
        content: normalizeOptional(payload.content),
        isPublic: payload.isPublic ?? undefined
      }
    });

    await this.logActivity(note.projectId, "PROJECT_NOTE_UPDATED", `Updated project note`, actor);
    return this.serializeNote(updated);
  }

  async deleteProjectNote(noteId: number, actor: string) {
    const note = await this.prisma.projectNote.findUnique({ where: { id: noteId } });
    if (!note) {
      throw new HttpError(404, "Project note not found");
    }

    await this.prisma.projectNote.delete({ where: { id: noteId } });
    await this.logActivity(note.projectId, "PROJECT_NOTE_DELETED", `Deleted project note`, actor);
  }

  async createTaskNote(taskId: number, payload: NotePayload, actor: string) {
    const task = await this.ensureTask(taskId);
    const note = await this.prisma.taskNote.create({
      data: {
        taskId,
        title: normalizeNullable(payload.title),
        content: normalizeNullable(payload.content),
        isPublic: payload.isPublic ?? true,
        ownerEmployeeId: actor,
        createdBy: actor
      }
    });
    await this.logActivity(task.projectId, "TASK_NOTE_CREATED", `Added task note`, actor, { taskId });
    return this.serializeNote(note);
  }

  async listTaskNotes(taskId: number, requesterId: string) {
    await this.ensureTask(taskId);
    const notes = await this.prisma.taskNote.findMany({
      where: {
        taskId,
        OR: [{ isPublic: true }, { ownerEmployeeId: requesterId }]
      },
      orderBy: { createdAt: "desc" }
    });
    return notes.map((note) => this.serializeNote(note));
  }

  async updateTaskNote(noteId: number, payload: NotePayload, actor: string) {
    const note = await this.prisma.taskNote.findUnique({ where: { id: noteId } });
    if (!note) {
      throw new HttpError(404, "Task note not found");
    }

    const task = await this.ensureTask(note.taskId);
    const updated = await this.prisma.taskNote.update({
      where: { id: noteId },
      data: {
        title: normalizeOptional(payload.title),
        content: normalizeOptional(payload.content),
        isPublic: payload.isPublic ?? undefined
      }
    });

    await this.logActivity(task.projectId, "TASK_NOTE_UPDATED", `Updated task note`, actor, { taskId: note.taskId });
    return this.serializeNote(updated);
  }

  async deleteTaskNote(noteId: number, actor: string) {
    const note = await this.prisma.taskNote.findUnique({ where: { id: noteId } });
    if (!note) {
      throw new HttpError(404, "Task note not found");
    }

    const task = await this.ensureTask(note.taskId);
    await this.prisma.taskNote.delete({ where: { id: noteId } });
    await this.logActivity(task.projectId, "TASK_NOTE_DELETED", `Deleted task note`, actor, { taskId: note.taskId });
  }

  async createTaskCategory(name: string, actor: string) {
    const normalizedName = normalizeRequired(name, "Task category name is required");
    return this.prisma.taskCategory.upsert({
      where: { name: normalizedName },
      update: {},
      create: {
        name: normalizedName,
        createdBy: actor
      }
    });
  }

  async listTaskCategories() {
    return this.prisma.taskCategory.findMany({ orderBy: { name: "asc" } });
  }

  async deleteTaskCategory(id: number) {
    await this.prisma.taskCategory.delete({ where: { id } });
  }

  async createTaskStage(payload: { name?: string; position?: number | string | null; labelColor?: string | null; projectId?: number | string | null }, actor: string) {
    return this.prisma.taskStage.create({
      data: {
        name: normalizeRequired(payload.name, "Task stage name is required"),
        position: toInt(payload.position),
        labelColor: normalizeNullable(payload.labelColor),
        projectId: toInt(payload.projectId),
        createdBy: actor
      }
    });
  }

  async updateTaskStage(id: number, payload: { name?: string; position?: number | string | null; labelColor?: string | null; projectId?: number | string | null }) {
    return this.prisma.taskStage.update({
      where: { id },
      data: {
        name: normalizeOptional(payload.name),
        position: optionalInt(payload.position),
        labelColor: normalizeOptional(payload.labelColor),
        projectId: optionalInt(payload.projectId)
      }
    });
  }

  async deleteTaskStage(id: number) {
    await this.prisma.taskStage.delete({ where: { id } });
  }

  async listTaskStages() {
    return this.prisma.taskStage.findMany({
      orderBy: [{ position: "asc" }, { name: "asc" }]
    });
  }

  async listTaskStagesForProject(projectId: number) {
    return this.prisma.taskStage.findMany({
      where: {
        OR: [{ projectId }, { projectId: null }]
      },
      orderBy: [{ position: "asc" }, { name: "asc" }]
    });
  }

  async createLabel(payload: { name?: string; colorCode?: string | null; projectId?: number | string | null; description?: string | null; projectName?: string | null }, actor: string) {
    return this.prisma.label.create({
      data: {
        name: normalizeRequired(payload.name, "Label name is required"),
        colorCode: normalizeNullable(payload.colorCode),
        projectId: toInt(payload.projectId),
        projectName: normalizeNullable(payload.projectName),
        description: normalizeNullable(payload.description),
        createdBy: actor
      }
    });
  }

  async updateLabel(id: number, payload: { name?: string; colorCode?: string | null; projectId?: number | string | null; description?: string | null; projectName?: string | null }) {
    return this.prisma.label.update({
      where: { id },
      data: {
        name: normalizeOptional(payload.name),
        colorCode: normalizeOptional(payload.colorCode),
        projectId: optionalInt(payload.projectId),
        projectName: normalizeOptional(payload.projectName),
        description: normalizeOptional(payload.description)
      }
    });
  }

  async deleteLabel(id: number) {
    await this.prisma.label.delete({ where: { id } });
  }

  async listLabels(projectId?: number) {
    return this.prisma.label.findMany({
      where: projectId ? { OR: [{ projectId }, { projectId: null }] } : undefined,
      orderBy: { name: "asc" }
    });
  }

  async createTask(payload: TaskPayload, actor: string, file?: UploadFileInput | null, isAdmin = true) {
    const projectId = toRequiredInt(payload.projectId, "projectId is required");
    const project = await this.ensureProject(projectId);
    const categoryId = await this.resolveTaskCategoryId(payload.category, actor);
    const labelIds = await this.resolveLabelIds(payload.labelIds);
    const assignedEmployeeIds = normalizeEmployeeIds(payload.assignedEmployeeIds);
    await this.ensureEmployeesExist(assignedEmployeeIds);
    const taskStageId = toInt(payload.taskStageId);
    const milestoneId = toInt(payload.milestoneId);
    if (milestoneId) {
      await this.ensureMilestone(projectId, milestoneId);
    }

    const task = await this.prisma.task.create({
      data: {
        title: normalizeRequired(payload.title, "Task title is required"),
        projectId,
        categoryId,
        startDate: parseDate(payload.startDate),
        dueDate: parseDate(payload.dueDate),
        noDueDate: normalizeBoolean(payload.noDueDate),
        taskStageId,
        assignedEmployeeIds,
        description: normalizeNullable(payload.description),
        milestoneId,
        priority: normalizeTaskPriority(payload.priority),
        isPrivate: normalizeBoolean(payload.isPrivate),
        timeEstimate: normalizeBoolean(payload.timeEstimate),
        timeEstimateMinutes: toInt(payload.timeEstimateMinutes),
        isDependent: normalizeBoolean(payload.isDependent),
        dependentTaskId: toInt(payload.dependentTaskId),
        statusEnum: !isAdmin && project.tasksNeedAdminApproval ? "WAITING" : undefined,
        approvedByAdmin: isAdmin || !project.tasksNeedAdminApproval,
        createdBy: actor,
        updatedBy: actor,
        labels: labelIds.length ? { connect: labelIds.map((id) => ({ id })) } : undefined
      },
      include: {
        labels: true,
        category: true,
        taskStage: true,
        milestone: true
      }
    });

    if (file) {
      await this.uploadTaskFile(task.id, file, actor);
    }

    await this.logActivity(projectId, "TASK_CREATED", `Task ${task.title} created`, actor, {
      taskId: task.id
    });
    return this.getTaskById(task.id, actor);
  }

  async updateTask(taskId: number, payload: TaskPayload, actor: string, file?: UploadFileInput | null) {
    const existing = await this.ensureTask(taskId);
    const categoryId = payload.category !== undefined ? await this.resolveTaskCategoryId(payload.category, actor) : undefined;
    const labelIds = payload.labelIds !== undefined ? await this.resolveLabelIds(payload.labelIds) : undefined;
    const assignedEmployeeIds =
      payload.assignedEmployeeIds !== undefined
        ? normalizeEmployeeIds(payload.assignedEmployeeIds)
        : undefined;

    if (assignedEmployeeIds) {
      await this.ensureEmployeesExist(assignedEmployeeIds);
    }

    const updated = await this.prisma.task.update({
      where: { id: taskId },
      data: {
        title: normalizeOptional(payload.title),
        categoryId,
        startDate: optionalDate(payload.startDate),
        dueDate: optionalDate(payload.dueDate),
        noDueDate: optionalBoolean(payload.noDueDate),
        taskStageId: optionalInt(payload.taskStageId),
        assignedEmployeeIds,
        description: normalizeOptional(payload.description),
        milestoneId: optionalInt(payload.milestoneId),
        priority: payload.priority ? normalizeTaskPriority(payload.priority) : undefined,
        isPrivate: optionalBoolean(payload.isPrivate),
        timeEstimate: optionalBoolean(payload.timeEstimate),
        timeEstimateMinutes: optionalInt(payload.timeEstimateMinutes),
        isDependent: optionalBoolean(payload.isDependent),
        dependentTaskId: optionalInt(payload.dependentTaskId),
        updatedBy: actor,
        labels:
          labelIds !== undefined
            ? {
                set: labelIds.map((id) => ({ id }))
              }
            : undefined
      }
    });

    if (file) {
      await this.uploadTaskFile(taskId, file, actor);
    }

    await this.logActivity(existing.projectId, "TASK_UPDATED", `Task ${updated.title} updated`, actor, {
      taskId
    });
    return this.getTaskById(taskId, actor);
  }

  async deleteTask(taskId: number, actor: string) {
    const task = await this.ensureTask(taskId);
    await this.prisma.task.delete({ where: { id: taskId } });
    await this.logActivity(task.projectId, "TASK_DELETED", `Task ${task.title} deleted`, actor, {
      taskId
    });
  }

  async listTasksByProject(projectId: number, requesterId: string) {
    await this.ensureProject(projectId);
    const tasks = await this.prisma.task.findMany({
      where: { projectId },
      include: {
        labels: true,
        category: true,
        taskStage: true,
        milestone: true
      },
      orderBy: { createdAt: "desc" }
    });
    return Promise.all(tasks.map((task) => this.enrichTask(task, requesterId)));
  }

  async listAssignedTasks(employeeId: string, requesterId: string) {
    const tasks = await this.prisma.task.findMany({
      where: {
        assignedEmployeeIds: { has: employeeId }
      },
      include: {
        labels: true,
        category: true,
        taskStage: true,
        milestone: true
      },
      orderBy: { createdAt: "desc" }
    });
    return Promise.all(tasks.map((task) => this.enrichTask(task, requesterId)));
  }

  async getTask(projectId: number, taskId: number, requesterId: string) {
    await this.ensureProject(projectId);
    const task = await this.ensureTask(taskId);
    if (task.projectId !== projectId) {
      throw new HttpError(404, "Task not found");
    }
    const fullTask = await this.prisma.task.findUniqueOrThrow({
      where: { id: taskId },
      include: {
        labels: true,
        category: true,
        taskStage: true,
        milestone: true
      }
    });
    return this.enrichTask(fullTask, requesterId);
  }

  async getTaskById(taskId: number, requesterId: string) {
    const task = await this.prisma.task.findUnique({
      where: { id: taskId },
      include: {
        labels: true,
        category: true,
        taskStage: true,
        milestone: true
      }
    });

    if (!task) {
      throw new HttpError(404, "Task not found");
    }

    return this.enrichTask(task, requesterId);
  }

  async getTaskCountsForEmployee(employeeId: string) {
    const pendingCount = await this.prisma.task.count({
      where: {
        assignedEmployeeIds: { has: employeeId },
        statusEnum: { not: "COMPLETE" }
      }
    });
    const overdueCount = await this.prisma.task.count({
      where: {
        assignedEmployeeIds: { has: employeeId },
        dueDate: { lt: startOfDay(new Date()) },
        statusEnum: { not: "COMPLETE" }
      }
    });
    return { pendingCount, overdueCount };
  }

  async getAllTaskCounts() {
    const pendingCount = await this.prisma.task.count({
      where: { statusEnum: { not: "COMPLETE" } }
    });
    const overdueCount = await this.prisma.task.count({
      where: {
        dueDate: { lt: startOfDay(new Date()) },
        statusEnum: { not: "COMPLETE" }
      }
    });
    return { pendingCount, overdueCount };
  }

  async listAllTasks(requesterId: string) {
    const tasks = await this.prisma.task.findMany({
      include: {
        labels: true,
        category: true,
        taskStage: true,
        milestone: true
      },
      orderBy: { createdAt: "desc" }
    });
    return Promise.all(tasks.map((task) => this.enrichTask(task, requesterId)));
  }

  async listWaitingTasks(requesterId: string) {
    const tasks = await this.prisma.task.findMany({
      where: {
        OR: [{ statusEnum: "WAITING" }, { approvedByAdmin: false }]
      },
      include: {
        labels: true,
        category: true,
        taskStage: true,
        milestone: true
      },
      orderBy: { createdAt: "desc" }
    });
    return Promise.all(tasks.map((task) => this.enrichTask(task, requesterId)));
  }

  async getAssignedTaskCount(employeeId: string) {
    const taskCount = await this.prisma.task.count({
      where: {
        assignedEmployeeIds: { has: employeeId }
      }
    });

    return {
      employeeId,
      taskCount
    };
  }

  async changeTaskStatus(taskId: number, statusId: number, actor: string) {
    const task = await this.ensureTask(taskId);
    const stage = await this.prisma.taskStage.findUnique({ where: { id: statusId } });
    if (!stage) {
      throw new HttpError(404, "Task stage not found");
    }

    const updated = await this.prisma.task.update({
      where: { id: taskId },
      data: {
        taskStageId: statusId,
        statusEnum: stage.name,
        updatedBy: actor,
        completedOn: stage.name.toUpperCase() === "COMPLETE" || stage.name.toUpperCase() === "COMPLETED" ? new Date() : null
      },
      include: {
        labels: true,
        category: true,
        taskStage: true,
        milestone: true
      }
    });

    await this.logActivity(task.projectId, "TASK_STATUS_UPDATED", `Task ${updated.title} moved to ${stage.name}`, actor, {
      taskId
    });
    return this.enrichTask(updated, actor);
  }

  async duplicateTask(taskId: number, actor: string) {
    const task = await this.prisma.task.findUnique({
      where: { id: taskId },
      include: { labels: true }
    });

    if (!task) {
      throw new HttpError(404, "Task not found");
    }

    const duplicate = await this.prisma.task.create({
      data: {
        title: `${task.title} (Copy)`,
        projectId: task.projectId,
        categoryId: task.categoryId,
        startDate: task.startDate,
        dueDate: task.dueDate,
        noDueDate: task.noDueDate,
        taskStageId: task.taskStageId,
        hoursLoggedMinutes: 0,
        assignedEmployeeIds: task.assignedEmployeeIds,
        description: task.description,
        milestoneId: task.milestoneId,
        priority: task.priority,
        isPrivate: task.isPrivate,
        timeEstimate: task.timeEstimate,
        timeEstimateMinutes: task.timeEstimateMinutes,
        isDependent: task.isDependent,
        dependentTaskId: task.dependentTaskId,
        duplicateOfTaskId: task.id,
        statusEnum: task.statusEnum,
        approvedByAdmin: task.approvedByAdmin,
        approvedAt: task.approvedAt,
        approvedBy: task.approvedBy,
        createdBy: actor,
        updatedBy: actor,
        labels: task.labels.length ? { connect: task.labels.map((label) => ({ id: label.id })) } : undefined
      }
    });

    await this.logActivity(task.projectId, "TASK_DUPLICATED", `Task ${task.title} duplicated`, actor, {
      taskId,
      duplicateTaskId: duplicate.id
    });

    return this.getTaskById(duplicate.id, actor);
  }

  async approveTask(taskId: number, actor: string) {
    const task = await this.ensureTask(taskId);
    const updated = await this.prisma.task.update({
      where: { id: taskId },
      data: {
        approvedByAdmin: true,
        approvedAt: new Date(),
        approvedBy: actor,
        statusEnum: task.statusEnum === "WAITING" ? null : task.statusEnum,
        updatedBy: actor
      },
      include: {
        labels: true,
        category: true,
        taskStage: true,
        milestone: true
      }
    });

    await this.logActivity(task.projectId, "TASK_APPROVED", `Task ${task.title} approved`, actor, {
      taskId
    });

    return this.enrichTask(updated, actor);
  }

  async getTaskCopyLinks(taskId: number) {
    const task = await this.ensureTask(taskId);
    return {
      taskId: task.id,
      projectId: task.projectId,
      appPath: `/tasks/${task.id}`,
      projectTaskPath: `/work/project/${task.projectId}?taskId=${task.id}`
    };
  }

  async createSubtask(taskId: number, payload: SubtaskPayload, actor: string) {
    const task = await this.ensureTask(taskId);
    const subtask = await this.prisma.subtask.create({
      data: {
        taskId,
        title: normalizeRequired(payload.title, "Subtask title is required"),
        description: normalizeNullable(payload.description),
        isDone: Boolean(payload.isDone),
        createdBy: actor,
        updatedBy: actor
      }
    });
    await this.logActivity(task.projectId, "SUBTASK_CREATED", `Subtask ${subtask.title} created`, actor, {
      taskId
    });
    return subtask;
  }

  async listSubtasks(taskId: number) {
    await this.ensureTask(taskId);
    return this.prisma.subtask.findMany({
      where: { taskId },
      orderBy: { createdAt: "desc" }
    });
  }

  async updateSubtask(subtaskId: number, payload: SubtaskPayload, actor: string) {
    const subtask = await this.prisma.subtask.findUnique({ where: { id: subtaskId } });
    if (!subtask) {
      throw new HttpError(404, "Subtask not found");
    }

    const updated = await this.prisma.subtask.update({
      where: { id: subtaskId },
      data: {
        title: normalizeOptional(payload.title),
        description: normalizeOptional(payload.description),
        isDone: payload.isDone ?? undefined,
        updatedBy: actor
      }
    });

    const task = await this.ensureTask(subtask.taskId);
    await this.logActivity(task.projectId, "SUBTASK_UPDATED", `Subtask ${updated.title} updated`, actor, {
      taskId: task.id
    });
    return updated;
  }

  async deleteSubtask(subtaskId: number, actor: string) {
    const subtask = await this.prisma.subtask.findUnique({ where: { id: subtaskId } });
    if (!subtask) {
      throw new HttpError(404, "Subtask not found");
    }
    const task = await this.ensureTask(subtask.taskId);
    await this.prisma.subtask.delete({ where: { id: subtaskId } });
    await this.logActivity(task.projectId, "SUBTASK_DELETED", `Subtask ${subtask.title} deleted`, actor, {
      taskId: task.id
    });
  }

  async toggleSubtask(subtaskId: number, actor: string) {
    const subtask = await this.prisma.subtask.findUnique({ where: { id: subtaskId } });
    if (!subtask) {
      throw new HttpError(404, "Subtask not found");
    }
    const updated = await this.prisma.subtask.update({
      where: { id: subtaskId },
      data: {
        isDone: !subtask.isDone,
        updatedBy: actor
      }
    });
    const task = await this.ensureTask(subtask.taskId);
    await this.logActivity(task.projectId, "SUBTASK_TOGGLED", `Subtask ${updated.title} toggled`, actor, {
      taskId: task.id
    });
    return updated;
  }

  async createTimeLog(payload: TimeLogPayload, actor: string) {
    const employeeId = normalizeRequired(payload.employeeId ?? actor, "employeeId is required");
    const startDate = parseDate(payload.startDate);
    if (!startDate) {
      throw new HttpError(400, "startDate is required");
    }

    const projectId = toInt(payload.projectId);
    const taskId = toInt(payload.taskId);
    const durationHours = payload.durationHours ?? calculateDurationHours(payload.startDate, payload.startTime, payload.endDate, payload.endTime);

    const timeLog = await this.prisma.timeLog.create({
      data: {
        projectId,
        taskId,
        employeeId,
        startDate,
        startTime: normalizeNullable(payload.startTime),
        endDate: parseDate(payload.endDate),
        endTime: normalizeNullable(payload.endTime),
        memo: normalizeNullable(payload.memo),
        durationHours,
        createdBy: actor
      }
    });

    if (taskId) {
      await this.recalculateTaskLoggedHours(taskId);
    }

    return this.enrichTimeLog(timeLog);
  }

  async updateTimeLog(id: number, payload: TimeLogPayload, actor: string) {
    const existing = await this.prisma.timeLog.findUnique({ where: { id } });
    if (!existing) {
      throw new HttpError(404, "Time log not found");
    }

    const durationHours = payload.durationHours ?? calculateDurationHours(
      payload.startDate ?? serializeDate(existing.startDate),
      payload.startTime ?? existing.startTime,
      payload.endDate ?? serializeDate(existing.endDate),
      payload.endTime ?? existing.endTime
    );

    const updated = await this.prisma.timeLog.update({
      where: { id },
      data: {
        projectId: optionalInt(payload.projectId),
        taskId: optionalInt(payload.taskId),
        employeeId: normalizeOptional(payload.employeeId),
        startDate: optionalDate(payload.startDate),
        startTime: normalizeOptional(payload.startTime),
        endDate: optionalDate(payload.endDate),
        endTime: normalizeOptional(payload.endTime),
        memo: normalizeOptional(payload.memo),
        durationHours,
        createdBy: actor
      }
    });

    if (existing.taskId) {
      await this.recalculateTaskLoggedHours(existing.taskId);
    }
    if (updated.taskId && updated.taskId !== existing.taskId) {
      await this.recalculateTaskLoggedHours(updated.taskId);
    }

    return this.enrichTimeLog(updated);
  }

  async deleteTimeLog(id: number, actor: string) {
    const existing = await this.prisma.timeLog.findUnique({ where: { id } });
    if (!existing) {
      throw new HttpError(404, "Time log not found");
    }
    await this.prisma.timeLog.delete({ where: { id } });
    if (existing.taskId) {
      await this.recalculateTaskLoggedHours(existing.taskId);
    }
  }

  async listAllTimeLogs() {
    const logs = await this.prisma.timeLog.findMany({ orderBy: { createdAt: "desc" } });
    return Promise.all(logs.map((log) => this.enrichTimeLog(log)));
  }

  async listTimeLogsByProject(projectId: number) {
    await this.ensureProject(projectId);
    const logs = await this.prisma.timeLog.findMany({
      where: { projectId },
      orderBy: { createdAt: "desc" }
    });
    return Promise.all(logs.map((log) => this.enrichTimeLog(log)));
  }

  async listTimeLogsByTask(taskId: number) {
    await this.ensureTask(taskId);
    const logs = await this.prisma.timeLog.findMany({
      where: { taskId },
      orderBy: { createdAt: "desc" }
    });
    return Promise.all(logs.map((log) => this.enrichTimeLog(log)));
  }

  async listTimeLogsByEmployee(employeeId: string) {
    const logs = await this.prisma.timeLog.findMany({
      where: { employeeId },
      orderBy: { createdAt: "desc" }
    });
    return Promise.all(logs.map((log) => this.enrichTimeLog(log)));
  }

  async getTotalHoursForEmployee(employeeId: string) {
    const result = await this.prisma.timeLog.aggregate({
      where: { employeeId },
      _sum: { durationHours: true }
    });
    return {
      employeeId,
      totalHours: result._sum.durationHours ?? 0
    };
  }

  async getTimeLogsForEmployeeOnDate(employeeId: string, date: string) {
    const day = parseDate(date);
    if (!day) {
      throw new HttpError(400, "date is required");
    }
    const nextDay = addDays(day, 1);
    const logs = await this.prisma.timeLog.findMany({
      where: {
        employeeId,
        startDate: {
          gte: day,
          lt: nextDay
        }
      },
      orderBy: { createdAt: "desc" }
    });

    const enriched = await Promise.all(logs.map((log) => this.enrichTimeLog(log)));
    return {
      date: serializeDate(day),
      logs: enriched,
      totalHours: enriched.reduce((sum, log) => sum + (log.durationHours ?? 0), 0)
    };
  }

  async getWeekSummaryForEmployee(employeeId: string, startDateValue: string) {
    const startDate = parseDate(startDateValue);
    if (!startDate) {
      throw new HttpError(400, "startDate is required");
    }

    const rows = await Promise.all(
      Array.from({ length: 7 }).map(async (_, offset) => {
        const day = addDays(startDate, offset);
        const result = await this.prisma.timeLog.aggregate({
          where: {
            employeeId,
            startDate: {
              gte: day,
              lt: addDays(day, 1)
            }
          },
          _sum: { durationHours: true }
        });

        return {
          date: serializeDate(day),
          totalHours: result._sum.durationHours ?? 0
        };
      })
    );

    return rows;
  }

  async createWeekly(actor: string, payload: WeeklyTimeLogPayload) {
    const employeeId = normalizeRequired(payload.employeeId ?? actor, "employeeId is required");
    const createdLogs = [];
    const alreadyFilledDates: string[] = [];
    const skippedInvalidDates: string[] = [];

    for (const day of payload.days ?? []) {
      const date = parseDate(day.date);
      const hours = day.hours === null || day.hours === undefined || day.hours === "" ? 0 : Number(day.hours);
      if (!date || Number.isNaN(hours) || hours <= 0) {
        if (day.date) {
          skippedInvalidDates.push(day.date);
        }
        continue;
      }

      const existing = await this.prisma.timeLog.findFirst({
        where: {
          employeeId,
          taskId: toInt(payload.taskId),
          startDate: {
            gte: date,
            lt: addDays(date, 1)
          }
        }
      });

      if (existing) {
        alreadyFilledDates.push(day.date ?? serializeDate(date) ?? "");
        continue;
      }

      const log = await this.prisma.timeLog.create({
        data: {
          projectId: toInt(payload.projectId),
          taskId: toInt(payload.taskId),
          employeeId,
          startDate: date,
          memo: normalizeNullable(day.memo),
          durationHours: hours,
          createdBy: actor
        }
      });

      createdLogs.push(await this.enrichTimeLog(log));
    }

    if (payload.taskId) {
      await this.recalculateTaskLoggedHours(toRequiredInt(payload.taskId, "taskId is required"));
    }

    return {
      employeeId,
      projectId: toInt(payload.projectId),
      taskId: toInt(payload.taskId),
      createdLogs,
      alreadyFilledDates,
      skippedInvalidDates
    };
  }

  async getAllEmployeesTimesheetSummary() {
    const logs = await this.prisma.timeLog.findMany();
    const grouped = new Map<string, number>();
    for (const log of logs) {
      grouped.set(log.employeeId, (grouped.get(log.employeeId) ?? 0) + (log.durationHours ?? 0));
    }

    return Promise.all(
      [...grouped.entries()].map(async ([employeeId, totalHours]) => {
        const employee = await this.employeeClient.getEmployeeMeta(employeeId);
        return {
          employeeId,
          employee,
          totalHours
        };
      })
    );
  }

  async getMyTimesheetSummary(employeeId: string) {
    const result = await this.prisma.timeLog.aggregate({
      where: { employeeId },
      _sum: { durationHours: true },
      _count: { id: true }
    });

    return {
      employeeId,
      totalHours: result._sum.durationHours ?? 0,
      totalEntries: result._count.id
    };
  }

  async createDiscussionCategory(payload: DiscussionCategoryPayload) {
    return this.prisma.discussionCategory.create({
      data: {
        categoryName: normalizeRequired(payload.categoryName, "categoryName is required"),
        colorCode: normalizeNullable(payload.colorCode)
      }
    });
  }

  async listDiscussionCategories() {
    return this.prisma.discussionCategory.findMany({ orderBy: { categoryName: "asc" } });
  }

  async deleteDiscussionCategory(id: number) {
    await this.prisma.discussionCategory.delete({ where: { id } });
  }

  async createDiscussionRoom(projectId: number, payload: DiscussionRoomPayload, actor: string, file?: UploadFileInput | null) {
    await this.ensureProject(projectId);
    const room = await this.prisma.discussionRoom.create({
      data: {
        title: normalizeRequired(payload.title, "Room title is required"),
        projectId,
        categoryId: toInt(payload.categoryId),
        createdBy: actor
      }
    });

    if (payload.initialMessage || file) {
      await this.createDiscussionMessage(room.id, {
        content: payload.initialMessage ?? ""
      }, actor, file);
    }

    return this.getDiscussionRoom(room.id);
  }

  async listDiscussionRooms(projectId: number) {
    await this.ensureProject(projectId);
    const rooms = await this.prisma.discussionRoom.findMany({
      where: { projectId },
      include: {
        category: true,
        messages: {
          where: { isDeleted: false },
          orderBy: { createdAt: "desc" },
          take: 1
        }
      },
      orderBy: { createdAt: "desc" }
    });

    return Promise.all(rooms.map((room) => this.serializeDiscussionRoom(room)));
  }

  async getDiscussionRoom(roomId: number) {
    const room = await this.prisma.discussionRoom.findUnique({
      where: { id: roomId },
      include: {
        category: true,
        messages: {
          where: { isDeleted: false },
          orderBy: { createdAt: "desc" },
          take: 1
        }
      }
    });

    if (!room) {
      throw new HttpError(404, "Discussion room not found");
    }

    return this.serializeDiscussionRoom(room);
  }

  async deleteDiscussionRoom(roomId: number) {
    await this.prisma.discussionRoom.delete({ where: { id: roomId } });
  }

  async createDiscussionMessage(roomId: number, payload: DiscussionMessagePayload, actor: string, file?: UploadFileInput | null) {
    await this.ensureDiscussionRoom(roomId);
    let uploaded: { url: string; objectKey: string | null } | null = null;
    if (file) {
      uploaded = await this.mediaStorageService.saveUploadedFile(file, `discussion/${roomId}`);
    }

    const message = await this.prisma.discussionMessage.create({
      data: {
        roomId,
        parentMessageId: toInt(payload.parentMessageId),
        senderId: actor,
        content: normalizeNullable(payload.content),
        messageType: file ? DiscussionMessageType.FILE : DiscussionMessageType.TEXT,
        filePath: uploaded?.objectKey ?? null,
        fileUrl: uploaded?.url ?? null,
        fileName: file?.filename ?? null,
        fileSize: file?.data.length ?? null,
        mimeType: file?.contentType ?? null
      }
    });

    return this.getDiscussionMessage(message.id);
  }

  async listDiscussionMessages(roomId: number) {
    await this.ensureDiscussionRoom(roomId);
    const messages = await this.prisma.discussionMessage.findMany({
      where: {
        roomId,
        parentMessageId: null
      },
      orderBy: { createdAt: "asc" }
    });
    return Promise.all(messages.map((message) => this.serializeDiscussionMessage(message)));
  }

  async getDiscussionMessage(messageId: number) {
    const message = await this.prisma.discussionMessage.findUnique({ where: { id: messageId } });
    if (!message) {
      throw new HttpError(404, "Discussion message not found");
    }
    return this.serializeDiscussionMessage(message);
  }

  async updateDiscussionMessage(messageId: number, content: string, actor: string) {
    const message = await this.prisma.discussionMessage.findUnique({ where: { id: messageId } });
    if (!message) {
      throw new HttpError(404, "Discussion message not found");
    }
    if (message.senderId !== actor) {
      throw new HttpError(403, "Only the sender can edit this message");
    }
    await this.prisma.discussionMessage.update({
      where: { id: messageId },
      data: { content }
    });
    return this.getDiscussionMessage(messageId);
  }

  async deleteDiscussionMessage(messageId: number, actor: string) {
    const message = await this.prisma.discussionMessage.findUnique({ where: { id: messageId } });
    if (!message) {
      throw new HttpError(404, "Discussion message not found");
    }
    if (message.senderId !== actor) {
      throw new HttpError(403, "Only the sender can delete this message");
    }
    await this.prisma.discussionMessage.update({
      where: { id: messageId },
      data: {
        isDeleted: true,
        deletedBy: actor,
        content: null
      }
    });
  }

  async replyToDiscussionMessage(parentMessageId: number, payload: DiscussionMessagePayload, actor: string) {
    const parent = await this.prisma.discussionMessage.findUnique({ where: { id: parentMessageId } });
    if (!parent) {
      throw new HttpError(404, "Parent message not found");
    }
    return this.createDiscussionMessage(parent.roomId, { ...payload, parentMessageId }, actor);
  }

  async listDiscussionReplies(parentMessageId: number) {
    const replies = await this.prisma.discussionMessage.findMany({
      where: { parentMessageId },
      orderBy: { createdAt: "asc" }
    });
    return Promise.all(replies.map((reply) => this.serializeDiscussionMessage(reply)));
  }

  async markBestReply(messageId: number) {
    const message = await this.prisma.discussionMessage.findUnique({ where: { id: messageId } });
    if (!message || !message.parentMessageId) {
      throw new HttpError(404, "Reply not found");
    }

    await this.prisma.discussionMessage.updateMany({
      where: { parentMessageId: message.parentMessageId },
      data: { isBestReply: false }
    });

    await this.prisma.discussionMessage.update({
      where: { id: messageId },
      data: { isBestReply: true }
    });

    return this.getDiscussionMessage(messageId);
  }

  async unmarkBestReply(messageId: number) {
    await this.prisma.discussionMessage.update({
      where: { id: messageId },
      data: { isBestReply: false }
    });
    return this.getDiscussionMessage(messageId);
  }

  private async enrichProject(project: Project, requesterId: string, includeMetrics = false) {
    const [client, assignedEmployees, projectAdmin, companyFiles, userState, metrics] = await Promise.all([
      project.clientId ? this.clientClient.getClientByClientId(project.clientId) : Promise.resolve<ClientMeta | null>(null),
      this.employeeClient.getEmployeesMeta(project.assignedEmployeeIds),
      project.projectAdminId ? this.employeeClient.getEmployeeMeta(project.projectAdminId) : Promise.resolve<EmployeeMeta | null>(null),
      this.prisma.fileMeta.findMany({
        where: { projectId: project.id, taskId: null, milestoneId: null },
        orderBy: { createdAt: "desc" }
      }),
      this.prisma.projectUserState.findUnique({
        where: {
          projectId_employeeId: {
            projectId: project.id,
            employeeId: requesterId
          }
        }
      }),
      includeMetrics ? this.getProjectMetrics(project.id) : Promise.resolve(null)
    ]);

    return {
      id: project.id,
      shortCode: project.shortCode,
      name: project.name,
      projectName: project.name,
      startDate: serializeDate(project.startDate),
      deadline: serializeDate(project.deadline),
      noDeadline: project.noDeadline,
      category: project.category,
      projectCategory: project.category,
      department: project.department,
      departmentId: project.department,
      clientId: project.clientId,
      client: mapClientMeta(client),
      summary: project.summary,
      projectSummary: project.summary,
      tasksNeedAdminApproval: project.tasksNeedAdminApproval,
      companyFiles: companyFiles.map((file) => this.serializeFile(file)),
      currency: project.currency,
      budget: decimalToNumber(project.budget),
      projectBudget: decimalToNumber(project.budget),
      hoursEstimate: project.hoursEstimate,
      allowManualTimeLogs: project.allowManualTimeLogs,
      addedBy: project.addedBy,
      assignedEmployeeIds: project.assignedEmployeeIds,
      assignedEmployees: assignedEmployees.map(mapEmployeeMeta),
      projectStatus: project.projectStatus,
      status: mapLegacyProjectStatus(project.projectStatus),
      progressPercent: project.progressPercent,
      progress: project.progressPercent,
      calculateProgressThroughTasks: project.calculateProgressThroughTasks,
      createdBy: project.createdBy,
      createdAt: project.createdAt.toISOString(),
      updatedBy: project.updatedBy,
      updatedAt: project.updatedAt.toISOString(),
      projectAdminId: project.projectAdminId,
      projectAdmin: mapEmployeeMeta(projectAdmin),
      isRequesterProjectAdmin: Boolean(project.projectAdminId && project.projectAdminId === requesterId),
      totalTimeLoggedMinutes: metrics?.totalTimeLoggedMinutes ?? project.cachedTotalTimeLogged ?? 0,
      expenses: metrics?.expenses ?? decimalToNumber(project.cachedExpenses),
      profit: metrics?.profit ?? decimalToNumber(project.cachedProfit),
      earning: metrics?.earning ?? decimalToNumber(project.budget),
      pinned: userState?.pinned ?? false,
      isPinned: userState?.pinned ?? false,
      pinnedAt: userState?.pinnedAt?.toISOString() ?? null,
      archived: userState?.archived ?? false,
      isArchived: userState?.archived ?? false,
      archivedAt: userState?.archivedAt?.toISOString() ?? null
    };
  }

  private async getProjectMetrics(projectId: number) {
    const [project, timeAggregate] = await Promise.all([
      this.ensureProject(projectId),
      this.prisma.timeLog.aggregate({
        where: { projectId },
        _sum: { durationHours: true }
      })
    ]);

    const totalHours = timeAggregate._sum.durationHours ?? 0;
    const totalTimeLoggedMinutes = Math.round(totalHours * 60);
    const expenses = 0;
    const earning = decimalToNumber(project.budget);
    const profit = Math.max(earning - expenses, 0);

    return {
      totalTimeLoggedMinutes,
      expenses,
      earning,
      profit,
      currency: project.currency,
      hoursEstimate: project.hoursEstimate
    };
  }

  private async enrichTask(
    task: Task & {
      labels?: Label[];
      category?: TaskCategory | null;
      taskStage?: TaskStage | null;
      milestone?: ProjectMilestone | null;
    },
    requesterId: string
  ) {
    const [project, assignedEmployees, attachments] = await Promise.all([
      this.prisma.project.findUnique({ where: { id: task.projectId } }),
      this.employeeClient.getEmployeesMeta(task.assignedEmployeeIds),
      this.prisma.fileMeta.findMany({
        where: { taskId: task.id },
        orderBy: { createdAt: "desc" }
      })
    ]);

    return {
      id: task.id,
      title: task.title,
      categoryId: task.category
        ? {
            id: task.category.id,
            name: task.category.name,
            createdBy: task.category.createdBy
          }
        : null,
      projectId: task.projectId,
      projectShortCode: project?.shortCode ?? null,
      projectName: project?.name ?? null,
      startDate: serializeDate(task.startDate),
      dueDate: serializeDate(task.dueDate),
      noDueDate: task.noDueDate,
      taskStage: task.taskStage
        ? {
            id: task.taskStage.id,
            name: task.taskStage.name,
            position: task.taskStage.position,
            labelColor: task.taskStage.labelColor,
            projectId: task.taskStage.projectId,
            createdBy: task.taskStage.createdBy
          }
        : null,
      taskStageId: task.taskStageId,
      assignedEmployeeIds: task.assignedEmployeeIds,
      assignedEmployees: assignedEmployees.map(mapEmployeeMeta),
      description: task.description,
      labels: (task.labels ?? []).map((label) => ({
        id: label.id,
        name: label.name,
        colorCode: label.colorCode,
        projectId: label.projectId,
        projectName: label.projectName,
        description: label.description,
        createdBy: label.createdBy
      })),
      milestone: task.milestone ? this.serializeMilestone(task.milestone) : null,
      milestoneId: task.milestoneId,
      priority: task.priority,
      isPrivate: task.isPrivate,
      timeEstimate: task.timeEstimate,
      timeEstimateMinutes: task.timeEstimateMinutes,
      isDependent: task.isDependent,
      dependentTaskId: task.dependentTaskId,
      attachments: attachments.map((attachment) => this.serializeFile(attachment)),
      createdBy: task.createdBy,
      createdAt: task.createdAt.toISOString(),
      updatedBy: task.updatedBy,
      updatedAt: task.updatedAt.toISOString(),
      pinned: false,
      pinnedAt: null,
      hoursLoggedMinutes: task.hoursLoggedMinutes,
      hoursLogged: roundHours((task.hoursLoggedMinutes ?? 0) / 60),
      completedOn: serializeDate(task.completedOn)
    };
  }

  private serializeMilestone(milestone: ProjectMilestone) {
    return {
      id: milestone.id,
      projectId: milestone.projectId,
      title: milestone.title,
      milestoneCost: decimalToNumber(milestone.milestoneCost),
      status: milestone.status,
      summary: milestone.summary,
      startDate: serializeDate(milestone.startDate),
      endDate: serializeDate(milestone.endDate),
      createdBy: milestone.createdBy,
      createdAt: milestone.createdAt.toISOString(),
      updatedBy: milestone.updatedBy,
      updatedAt: milestone.updatedAt.toISOString()
    };
  }

  private serializeFile(file: FileMeta) {
    return {
      id: file.id,
      projectId: file.projectId,
      taskId: file.taskId,
      milestoneId: file.milestoneId,
      filename: file.filename,
      bucket: file.bucket,
      path: file.path,
      url: file.url,
      mimeType: file.mimeType,
      size: file.size,
      uploadedBy: file.uploadedBy,
      createdAt: file.createdAt.toISOString()
    };
  }

  private serializeNote(note: ProjectNote | TaskNote) {
    return {
      id: note.id,
      title: note.title,
      content: note.content,
      isPublic: note.isPublic,
      createdBy: note.createdBy,
      createdAt: note.createdAt.toISOString()
    };
  }

  private async enrichTimeLog(timeLog: TimeLog) {
    const [project, task, employee] = await Promise.all([
      timeLog.projectId ? this.prisma.project.findUnique({ where: { id: timeLog.projectId } }) : Promise.resolve(null),
      timeLog.taskId ? this.prisma.task.findUnique({ where: { id: timeLog.taskId } }) : Promise.resolve(null),
      this.employeeClient.getEmployeeMeta(timeLog.employeeId)
    ]);

    return {
      id: timeLog.id,
      projectId: timeLog.projectId,
      projectShortCode: project?.shortCode ?? null,
      projectName: project?.name ?? null,
      taskId: timeLog.taskId,
      taskName: task?.title ?? null,
      employeeId: timeLog.employeeId,
      employees: employee ? [employee] : [],
      startDate: serializeDate(timeLog.startDate),
      startTime: timeLog.startTime,
      endDate: serializeDate(timeLog.endDate),
      endTime: timeLog.endTime,
      memo: timeLog.memo,
      durationHours: timeLog.durationHours,
      createdBy: timeLog.createdBy,
      createdAt: timeLog.createdAt.toISOString()
    };
  }

  private async serializeDiscussionRoom(
    room: DiscussionRoom & {
      category?: DiscussionCategory | null;
      messages?: DiscussionMessage[];
    }
  ): Promise<Record<string, unknown>> {
    const [createdByUser, lastMessage] = await Promise.all([
      this.employeeClient.getEmployeeMeta(room.createdBy),
      room.messages?.[0] ? this.serializeDiscussionMessage(room.messages[0]) : Promise.resolve(null)
    ]);

    return {
      id: room.id,
      title: room.title,
      projectId: room.projectId,
      category: room.category
        ? {
            id: room.category.id,
            categoryName: room.category.categoryName,
            colorCode: room.category.colorCode
          }
        : null,
      createdBy: room.createdBy,
      createdByUser,
      createdAt: room.createdAt.toISOString(),
      updatedAt: room.updatedAt.toISOString(),
      isActive: room.isActive,
      messageCount: await this.prisma.discussionMessage.count({ where: { roomId: room.id, isDeleted: false } }),
      lastMessage
    };
  }

  private async serializeDiscussionMessage(message: DiscussionMessage): Promise<Record<string, unknown>> {
    const [sender, replies] = await Promise.all([
      this.employeeClient.getEmployeeMeta(message.senderId),
      this.prisma.discussionMessage.findMany({
        where: { parentMessageId: message.id },
        orderBy: { createdAt: "asc" }
      })
    ]);

    return {
      id: message.id,
      content: message.content,
      roomId: message.roomId,
      parentMessageId: message.parentMessageId,
      senderId: message.senderId,
      sender,
      messageType: message.messageType,
      filePath: message.filePath,
      fileUrl: message.fileUrl,
      fileName: message.fileName,
      fileSize: message.fileSize,
      mimeType: message.mimeType,
      createdAt: message.createdAt.toISOString(),
      updatedAt: message.updatedAt.toISOString(),
      isDeleted: message.isDeleted,
      deletedBy: message.deletedBy,
      replies: await Promise.all(replies.map((reply) => this.serializeDiscussionMessage(reply))),
      replyCount: replies.length,
      isBestReply: message.isBestReply
    };
  }

  private async ensureProject(projectId: number) {
    const project = await this.prisma.project.findUnique({ where: { id: projectId } });
    if (!project) {
      throw new HttpError(404, "Project not found");
    }
    return project;
  }

  private async ensureTask(taskId: number) {
    const task = await this.prisma.task.findUnique({ where: { id: taskId } });
    if (!task) {
      throw new HttpError(404, "Task not found");
    }
    return task;
  }

  private async ensureMilestone(projectId: number, milestoneId: number) {
    const milestone = await this.prisma.projectMilestone.findUnique({ where: { id: milestoneId } });
    if (!milestone || milestone.projectId !== projectId) {
      throw new HttpError(404, "Milestone not found");
    }
    return milestone;
  }

  private async ensureDiscussionRoom(roomId: number) {
    const room = await this.prisma.discussionRoom.findUnique({ where: { id: roomId } });
    if (!room) {
      throw new HttpError(404, "Discussion room not found");
    }
    return room;
  }

  private async saveProjectFile(projectId: number, file: UploadFileInput, actor: string, folder: string) {
    const uploaded = await this.mediaStorageService.saveUploadedFile(file, `projects/${projectId}/${folder}`);
    if (!uploaded) {
      throw new HttpError(400, "File is required");
    }

    const fileMeta = await this.prisma.fileMeta.create({
      data: {
        projectId,
        filename: file.filename ?? "file",
        bucket: "cloudinary",
        path: uploaded.objectKey ?? uploaded.url,
        url: uploaded.url,
        mimeType: file.contentType,
        size: file.data.length,
        objectKey: uploaded.objectKey,
        uploadedBy: actor
      }
    });

    return this.serializeFile(fileMeta);
  }

  private async ensureEmployeesExist(employeeIds: string[]) {
    await Promise.all(employeeIds.map((employeeId) => this.employeeClient.ensureEmployeeExists(employeeId)));
  }

  private async resolveTaskCategoryId(value: string | number | null | undefined, actor: string) {
    const normalized = normalizeNullable(value);
    if (!normalized) {
      return null;
    }
    const numeric = Number(normalized);
    if (!Number.isNaN(numeric)) {
      const category = await this.prisma.taskCategory.findUnique({ where: { id: numeric } });
      if (!category) {
        throw new HttpError(404, "Task category not found");
      }
      return category.id;
    }

    const category = await this.prisma.taskCategory.upsert({
      where: { name: normalized },
      update: {},
      create: {
        name: normalized,
        createdBy: actor
      }
    });

    return category.id;
  }

  private async resolveLabelIds(value: Array<string | number> | string | null | undefined) {
    const ids = normalizeNumericIds(value);
    if (!ids.length) {
      return [];
    }

    const labels = await this.prisma.label.findMany({
      where: { id: { in: ids } },
      select: { id: true }
    });
    return labels.map((label) => label.id);
  }

  private async recalculateTaskLoggedHours(taskId: number) {
    const aggregate = await this.prisma.timeLog.aggregate({
      where: { taskId },
      _sum: { durationHours: true }
    });

    const totalMinutes = Math.round((aggregate._sum.durationHours ?? 0) * 60);

    await this.prisma.task.update({
      where: { id: taskId },
      data: {
        hoursLoggedMinutes: totalMinutes
      }
    });
  }

  private async logActivity(projectId: number, type: string, message: string, actorId: string, metadata?: Record<string, unknown>) {
    await this.prisma.projectActivity.create({
      data: {
        projectId,
        type,
        message,
        actorId,
        metadata: metadata as Prisma.InputJsonValue | undefined
      }
    });
  }

  private async enrichProjectById(projectId: number, requesterId: string) {
    const project = await this.prisma.project.findUnique({ where: { id: projectId } });
    if (!project) {
      return null;
    }
    return this.enrichProject(project, requesterId);
  }
}

function normalizeRequired(value: unknown, message: string): string {
  const normalized = normalizeNullable(value);
  if (!normalized) {
    throw new HttpError(400, message);
  }
  return normalized;
}

function normalizeNullable(value: unknown): string | null {
  if (value === null || value === undefined) {
    return null;
  }
  const normalized = String(value).trim();
  return normalized ? normalized : null;
}

function normalizeOptional(value: unknown): string | undefined {
  if (value === undefined) {
    return undefined;
  }
  return normalizeNullable(value) ?? undefined;
}

function normalizeBoolean(value: unknown): boolean {
  if (typeof value === "boolean") {
    return value;
  }
  return String(value).toLowerCase() === "true";
}

function optionalBoolean(value: unknown): boolean | undefined {
  return value === undefined ? undefined : normalizeBoolean(value);
}

function toInt(value: unknown): number | null {
  const normalized = normalizeNullable(value);
  if (!normalized) {
    return null;
  }
  const parsed = Number(normalized);
  return Number.isNaN(parsed) ? null : parsed;
}

function toRequiredInt(value: unknown, message: string): number {
  const parsed = toInt(value);
  if (parsed === null) {
    throw new HttpError(400, message);
  }
  return parsed;
}

function optionalInt(value: unknown): number | undefined {
  const parsed = value === undefined ? undefined : toInt(value);
  return parsed ?? undefined;
}

function toDecimal(value: unknown): number | null {
  const normalized = normalizeNullable(value);
  if (!normalized) {
    return null;
  }
  const parsed = Number(normalized);
  return Number.isNaN(parsed) ? null : parsed;
}

function optionalDecimal(value: unknown): number | undefined {
  const parsed = value === undefined ? undefined : toDecimal(value);
  return parsed ?? undefined;
}

function decimalToNumber(value: unknown): number {
  if (value === null || value === undefined) {
    return 0;
  }
  return Number(value);
}

function parseDate(value: unknown): Date | null {
  const normalized = normalizeNullable(value);
  if (!normalized) {
    return null;
  }
  const parsed = new Date(normalized);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function optionalDate(value: unknown): Date | undefined {
  const parsed = value === undefined ? undefined : parseDate(value);
  return parsed ?? undefined;
}

function serializeDate(value: Date | null | undefined): string | null {
  return value ? value.toISOString() : null;
}

function clampPercent(value: unknown): number {
  const parsed = Number(value ?? 0);
  if (Number.isNaN(parsed)) {
    return 0;
  }
  return Math.min(100, Math.max(0, Math.round(parsed)));
}

function normalizeEmployeeIds(value: string[] | string | null | undefined): string[] {
  if (!value) {
    return [];
  }
  const raw =
    Array.isArray(value)
      ? value
      : typeof value === "string" && value.trim().startsWith("[")
        ? (JSON.parse(value) as string[])
        : String(value).split(",");
  return [...new Set(raw.map((entry) => String(entry).trim()).filter(Boolean))];
}

function normalizeNumericIds(value: Array<string | number> | string | null | undefined): number[] {
  if (!value) {
    return [];
  }

  const raw =
    Array.isArray(value)
      ? value
      : typeof value === "string" && value.trim().startsWith("[")
        ? (JSON.parse(value) as Array<string | number>)
        : String(value).split(",");

  return [...new Set(raw.map((entry) => Number(String(entry).trim())).filter((entry) => !Number.isNaN(entry)))];
}

function normalizeProjectStatus(value: unknown): ProjectStatus {
  const normalized = (normalizeNullable(value) ?? "NOT_STARTED").toUpperCase();
  switch (normalized) {
    case "IN_PROGRESS":
      return ProjectStatus.IN_PROGRESS;
    case "ON_HOLD":
      return ProjectStatus.ON_HOLD;
    case "CANCELLED":
      return ProjectStatus.CANCELLED;
    case "FINISHED":
    case "COMPLETED":
      return ProjectStatus.FINISHED;
    default:
      return ProjectStatus.NOT_STARTED;
  }
}

function normalizeMilestoneStatus(value: unknown): MilestoneStatus {
  return (normalizeNullable(value) ?? "INCOMPLETE").toUpperCase() === "COMPLETED"
    ? MilestoneStatus.COMPLETED
    : MilestoneStatus.INCOMPLETE;
}

function normalizeTaskPriority(value: unknown): TaskPriority {
  const normalized = (normalizeNullable(value) ?? "MEDIUM").toUpperCase();
  switch (normalized) {
    case "LOW":
      return TaskPriority.LOW;
    case "HIGH":
      return TaskPriority.HIGH;
    case "URGENT":
      return TaskPriority.URGENT;
    default:
      return TaskPriority.MEDIUM;
  }
}

function calculateDurationHours(
  startDate: unknown,
  startTime: unknown,
  endDate: unknown,
  endTime: unknown
): number | null {
  const startDateValue = normalizeNullable(startDate);
  const endDateValue = normalizeNullable(endDate);
  const startTimeValue = normalizeNullable(startTime);
  const endTimeValue = normalizeNullable(endTime);
  if (!startDateValue || !endDateValue || !startTimeValue || !endTimeValue) {
    return null;
  }

  const start = new Date(`${startDateValue}T${startTimeValue}`);
  const end = new Date(`${endDateValue}T${endTimeValue}`);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end <= start) {
    return null;
  }

  return roundHours((end.getTime() - start.getTime()) / 36e5);
}

function roundHours(value: number): number {
  return Math.round(value * 100) / 100;
}

function addDays(date: Date, days: number): Date {
  const next = new Date(date);
  next.setUTCDate(next.getUTCDate() + days);
  return next;
}

function startOfDay(date: Date): Date {
  return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
}

function mapEmployeeMeta(employee: EmployeeMeta | null) {
  if (!employee) {
    return null;
  }

  return {
    ...employee,
    department: employee.departmentName ?? null,
    designation: employee.designationName ?? null,
    profileUrl: employee.profilePictureUrl ?? null
  };
}

function mapClientMeta(client: ClientMeta | null) {
  if (!client) {
    return null;
  }

  return {
    ...client,
    profilePictureUrl: client.profilePictureUrl ?? client.companyLogoUrl ?? null
  };
}

function mapLegacyProjectStatus(status: ProjectStatus): string {
  switch (status) {
    case ProjectStatus.FINISHED:
      return "completed";
    case ProjectStatus.ON_HOLD:
      return "on-hold";
    case ProjectStatus.CANCELLED:
      return "cancelled";
    case ProjectStatus.IN_PROGRESS:
      return "active";
    case ProjectStatus.NOT_STARTED:
    default:
      return "planning";
  }
}
