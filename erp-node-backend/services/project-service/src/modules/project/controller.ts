import type { IncomingMessage, ServerResponse } from "node:http";
import { URL } from "node:url";

import type { AuthContext } from "@erp/shared-auth";

import { getAuthContext, requireAdmin, requireEmployeeOrAdmin } from "../../common/auth.js";
import { HttpError } from "../../common/errors.js";
import { parseMultipartFormData, readJsonBody, sendJson, type MultipartFieldFile } from "../../common/http.js";
import type { ProjectConfig } from "../../config/env.js";
import type {
  MilestonePayload,
  NotePayload,
  ProjectPayload,
  ProjectService,
  SubtaskPayload,
  TaskPayload
} from "../../services/project.service.js";

interface EmployeeAssignmentRequest {
  employeeIds?: string[] | string | null;
}

interface StatusPayload {
  status?: string | null;
}

interface ProgressPayload {
  percent?: number | string | null;
}

interface SimpleNamePayload {
  name?: string | null;
  categoryName?: string | null;
}

interface TaskStagePayload {
  name?: string;
  position?: number | string | null;
  labelColor?: string | null;
  projectId?: number | string | null;
}

interface LabelPayload {
  name?: string;
  colorCode?: string | null;
  projectId?: number | string | null;
  description?: string | null;
  projectName?: string | null;
}

export async function handleProjectRoutes(
  request: IncomingMessage,
  response: ServerResponse,
  service: ProjectService,
  config: ProjectConfig
): Promise<boolean> {
  const method = request.method ?? "GET";
  const url = new URL(request.url ?? "/", "http://localhost");
  const pathname = url.pathname;
  const auth = () => getAuthContext(request.headers.authorization, config.jwtSecret);

  try {
    if (method === "POST" && pathname === "/api/projects") {
      const context = auth();
      requireAdmin(context);
      const { payload, companyFile } = await readProjectMultipartPayload(request);
      sendJson(response, 201, await service.createProject(payload, context.userId, companyFile));
      return true;
    }

    if (method === "GET" && pathname === "/api/projects") {
      const context = auth();
      requireAdmin(context);
      sendJson(response, 200, await service.getAll(context.userId));
      return true;
    }

    if (method === "GET" && pathname === "/api/projects/AllProject") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.getAll(context.userId));
      return true;
    }

    if (method === "GET" && pathname === "/api/projects/counts") {
      const context = auth();
      requireAdmin(context);
      sendJson(response, 200, await service.getProjectCounts());
      return true;
    }

    const adminProjectByIdMatch = pathname.match(/^\/api\/projects\/(\d+)$/);
    if (adminProjectByIdMatch && method === "GET") {
      const context = auth();
      requireAdmin(context);
      sendJson(response, 200, await service.getProject(Number(adminProjectByIdMatch[1]), context.userId));
      return true;
    }

    if (adminProjectByIdMatch && (method === "PUT" || method === "PATCH")) {
      const context = auth();
      requireAdmin(context);
      const { payload, companyFile } = await readProjectUpdatePayload(request);
      sendJson(
        response,
        200,
        await service.updateProject(Number(adminProjectByIdMatch[1]), payload, context.userId, companyFile)
      );
      return true;
    }

    if (adminProjectByIdMatch && method === "DELETE") {
      const context = auth();
      requireAdmin(context);
      await service.deleteProject(Number(adminProjectByIdMatch[1]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    const assignEmployeesMatch = pathname.match(/^\/api\/projects\/(\d+)\/assign$/);
    if (assignEmployeesMatch && method === "POST") {
      const context = auth();
      requireAdmin(context);
      const body = await readJsonBody<EmployeeAssignmentRequest>(request);
      await service.addAssignedEmployees(
        Number(assignEmployeesMatch[1]),
        normalizeStringArray(body.employeeIds),
        context.userId
      );
      response.writeHead(204);
      response.end();
      return true;
    }

    const removeAssignedEmployeeMatch = pathname.match(/^\/api\/projects\/(\d+)\/assign\/([^/]+)$/);
    if (removeAssignedEmployeeMatch && method === "DELETE") {
      const context = auth();
      requireAdmin(context);
      await service.removeAssignedEmployee(
        Number(removeAssignedEmployeeMatch[1]),
        decodeURIComponent(removeAssignedEmployeeMatch[2]),
        context.userId
      );
      response.writeHead(204);
      response.end();
      return true;
    }

    const projectStatusMatch = pathname.match(/^\/api\/projects\/(\d+)\/status$/);
    if (projectStatusMatch && method === "PUT") {
      const context = auth();
      requireAdmin(context);
      const body = isJsonRequest(request) ? await readJsonBody<StatusPayload>(request) : {};
      const status = url.searchParams.get("status") ?? body.status;
      if (!status) {
        throw new HttpError(400, "status query param is required");
      }
      await service.updateStatus(Number(projectStatusMatch[1]), status, context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    const projectProgressMatch = pathname.match(/^\/api\/projects\/(\d+)\/progress$/);
    if (projectProgressMatch && method === "PUT") {
      const context = auth();
      requireAdmin(context);
      const body = isJsonRequest(request) ? await readJsonBody<ProgressPayload>(request) : {};
      const percent = url.searchParams.get("percent") ?? body.percent;
      if (percent === undefined || percent === null || percent === "") {
        throw new HttpError(400, "percent query param is required");
      }
      await service.updateProgress(Number(projectProgressMatch[1]), percent, context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    if (method === "POST" && pathname === "/api/projects/import/csv") {
      const context = auth();
      requireAdmin(context);
      const multipart = await parseMultipartFormData(request);
      const file = firstFile(multipart.files, "file", "csv", "projectFile");
      sendJson(response, 200, await service.importProjectsFromCsv(file, context.userId));
      return true;
    }

    const projectsByEmployeeMatch = pathname.match(/^\/api\/projects\/employee\/([^/]+)$/);
    if (projectsByEmployeeMatch && method === "GET") {
      const context = auth();
      requireAdmin(context);
      sendJson(
        response,
        200,
        await service.listProjectsForEmployee(decodeURIComponent(projectsByEmployeeMatch[1]), context.userId)
      );
      return true;
    }

    const projectsByEmployeeStatsMatch = pathname.match(/^\/api\/projects\/employee\/([^/]+)\/stats\/count$/);
    if (projectsByEmployeeStatsMatch && method === "GET") {
      const context = auth();
      requireAdmin(context);
      sendJson(
        response,
        200,
        await service.getProjectCountForEmployee(decodeURIComponent(projectsByEmployeeStatsMatch[1]))
      );
      return true;
    }

    const projectsByClientMatch = pathname.match(/^\/api\/projects\/client\/([^/]+)$/);
    if (projectsByClientMatch && method === "GET") {
      const context = auth();
      requireAdmin(context);
      sendJson(
        response,
        200,
        await service.listProjectsByClient(decodeURIComponent(projectsByClientMatch[1]), context.userId)
      );
      return true;
    }

    const projectsByClientStatsMatch = pathname.match(/^\/api\/projects\/client\/([^/]+)\/stats$/);
    if (projectsByClientStatsMatch && method === "GET") {
      const context = auth();
      requireAdmin(context);
      sendJson(response, 200, await service.getClientProjectStats(decodeURIComponent(projectsByClientStatsMatch[1])));
      return true;
    }

    if (method === "POST" && pathname === "/api/projects/category") {
      const context = auth();
      requireAdmin(context);
      const body = await readJsonBody<SimpleNamePayload>(request);
      sendJson(response, 201, await service.createProjectCategory(body.categoryName ?? body.name ?? ""));
      return true;
    }

    if (method === "GET" && pathname === "/api/projects/category") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listProjectCategories());
      return true;
    }

    const projectCategoryDeleteMatch = pathname.match(/^\/api\/projects\/category\/(\d+)$/);
    if (projectCategoryDeleteMatch && method === "DELETE") {
      const context = auth();
      requireAdmin(context);
      await service.deleteProjectCategory(Number(projectCategoryDeleteMatch[1]));
      response.writeHead(204);
      response.end();
      return true;
    }

    if (method === "GET" && pathname === "/projects") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listProjectsForEmployee(context.userId, context.userId));
      return true;
    }

    if (method === "GET" && pathname === "/projects/AllProject") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.getAll(context.userId));
      return true;
    }

    if (method === "GET" && pathname === "/projects/counts/me") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.getProjectCountsForEmployee(context.userId));
      return true;
    }

    if (method === "GET" && pathname === "/projects/pinned") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listPinnedProjectDetails(context.userId));
      return true;
    }

    if (method === "GET" && pathname === "/projects/archived") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listArchivedProjectDetails(context.userId));
      return true;
    }

    const publicProjectByIdMatch = pathname.match(/^\/projects\/(\d+)$/);
    if (publicProjectByIdMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.getProject(Number(publicProjectByIdMatch[1]), context.userId));
      return true;
    }

    const projectMetricsMatch = pathname.match(/^\/projects\/(\d+)\/metrics$/);
    if (projectMetricsMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.getProjectWithMetrics(Number(projectMetricsMatch[1]), context.userId));
      return true;
    }

    const projectAdminMatch = pathname.match(/^\/projects\/(\d+)\/admin$/);
    if (projectAdminMatch && method === "POST") {
      const context = auth();
      requireAdmin(context);
      const userId = url.searchParams.get("userId");
      if (!userId) {
        throw new HttpError(400, "userId query param is required");
      }
      await service.assignProjectAdmin(Number(projectAdminMatch[1]), userId, context.userId);
      sendJson(response, 200, await service.getProject(Number(projectAdminMatch[1]), context.userId));
      return true;
    }

    if (projectAdminMatch && method === "DELETE") {
      const context = auth();
      requireAdmin(context);
      await service.removeProjectAdmin(Number(projectAdminMatch[1]), context.userId);
      sendJson(response, 200, await service.getProject(Number(projectAdminMatch[1]), context.userId));
      return true;
    }

    const pinMatch = pathname.match(/^\/projects\/(\d+)\/pin$/);
    if (pinMatch && method === "POST") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      await service.pinProject(Number(pinMatch[1]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    if (pinMatch && method === "DELETE") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      await service.unpinProject(Number(pinMatch[1]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    const archiveMatch = pathname.match(/^\/projects\/(\d+)\/archive$/);
    if (archiveMatch && method === "POST") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      await service.archiveProject(Number(archiveMatch[1]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    if (archiveMatch && method === "DELETE") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      await service.unarchiveProject(Number(archiveMatch[1]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    const projectActivityMatch = pathname.match(/^\/projects\/(\d+)\/activity$/);
    if (projectActivityMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listProjectActivity(Number(projectActivityMatch[1])));
      return true;
    }

    const adminMilestoneCollectionMatch = pathname.match(/^\/api\/projects\/(\d+)\/milestones$/);
    if (adminMilestoneCollectionMatch && method === "POST") {
      const context = auth();
      requireAdmin(context);
      const body = await readJsonBody<MilestonePayload>(request);
      sendJson(response, 201, await service.createMilestone(Number(adminMilestoneCollectionMatch[1]), body, context.userId));
      return true;
    }

    if (adminMilestoneCollectionMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listMilestones(Number(adminMilestoneCollectionMatch[1])));
      return true;
    }

    const adminMilestoneByIdMatch = pathname.match(/^\/api\/projects\/(\d+)\/milestones\/(\d+)$/);
    if (adminMilestoneByIdMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(
        response,
        200,
        await service.getMilestone(Number(adminMilestoneByIdMatch[1]), Number(adminMilestoneByIdMatch[2]))
      );
      return true;
    }

    if (adminMilestoneByIdMatch && method === "PUT") {
      const context = auth();
      requireAdmin(context);
      const body = await readJsonBody<MilestonePayload>(request);
      sendJson(
        response,
        200,
        await service.updateMilestone(
          Number(adminMilestoneByIdMatch[1]),
          Number(adminMilestoneByIdMatch[2]),
          body,
          context.userId
        )
      );
      return true;
    }

    if (adminMilestoneByIdMatch && method === "DELETE") {
      const context = auth();
      requireAdmin(context);
      await service.deleteMilestone(Number(adminMilestoneByIdMatch[1]), Number(adminMilestoneByIdMatch[2]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    const milestoneStatusMatch = pathname.match(/^\/api\/projects\/(\d+)\/milestones\/(\d+)\/status$/);
    if (milestoneStatusMatch && method === "PATCH") {
      const context = auth();
      requireAdmin(context);
      const body = isJsonRequest(request) ? await readJsonBody<StatusPayload>(request) : {};
      const status = url.searchParams.get("status") ?? body.status;
      if (!status) {
        throw new HttpError(400, "status is required");
      }
      sendJson(
        response,
        200,
        await service.updateMilestoneStatus(
          Number(milestoneStatusMatch[1]),
          Number(milestoneStatusMatch[2]),
          status,
          context.userId
        )
      );
      return true;
    }

    const publicMilestoneCollectionMatch = pathname.match(/^\/projects\/(\d+)\/milestones$/);
    if (publicMilestoneCollectionMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listMilestones(Number(publicMilestoneCollectionMatch[1])));
      return true;
    }

    const publicMilestoneByIdMatch = pathname.match(/^\/projects\/(\d+)\/milestones\/(\d+)$/);
    if (publicMilestoneByIdMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(
        response,
        200,
        await service.getMilestone(Number(publicMilestoneByIdMatch[1]), Number(publicMilestoneByIdMatch[2]))
      );
      return true;
    }

    const projectFilesMatch = pathname.match(/^\/files\/projects\/(\d+)$/);
    if (projectFilesMatch && method === "POST") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const multipart = await parseMultipartFormData(request);
      const file = firstFile(multipart.files, "file", "projectFile");
      if (!file) {
        throw new HttpError(400, "file is required");
      }
      sendJson(response, 201, await service.uploadProjectFile(Number(projectFilesMatch[1]), file, context.userId));
      return true;
    }

    if (projectFilesMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listProjectFiles(Number(projectFilesMatch[1])));
      return true;
    }

    const taskFilesMatch = pathname.match(/^\/files\/tasks\/(\d+)$/);
    if (taskFilesMatch && method === "POST") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const multipart = await parseMultipartFormData(request);
      const file = firstFile(multipart.files, "file", "taskFile");
      if (!file) {
        throw new HttpError(400, "file is required");
      }
      sendJson(response, 201, await service.uploadTaskFile(Number(taskFilesMatch[1]), file, context.userId));
      return true;
    }

    if (taskFilesMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listTaskFiles(Number(taskFilesMatch[1])));
      return true;
    }

    const fileDeleteAliasMatch = pathname.match(/^\/files\/projects\/(\d+)\/(\d+)$/);
    if (fileDeleteAliasMatch && method === "DELETE") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      await service.deleteFile(Number(fileDeleteAliasMatch[2]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    const fileByIdMatch = pathname.match(/^\/files\/(\d+)$/);
    if (fileByIdMatch && method === "DELETE") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      await service.deleteFile(Number(fileByIdMatch[1]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    const fileDownloadMatch = pathname.match(/^\/files\/(\d+)\/download$/);
    if (fileDownloadMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const file = await service.getFileDownload(Number(fileDownloadMatch[1]));
      if (!file.url) {
        throw new HttpError(404, "File URL not found");
      }
      response.writeHead(302, { location: file.url });
      response.end();
      return true;
    }

    const taskNotesMatch = pathname.match(/^\/tasks\/(\d+)\/notes$/);
    if (taskNotesMatch && method === "POST") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const body = await readJsonBody<NotePayload>(request);
      sendJson(response, 201, await service.createTaskNote(Number(taskNotesMatch[1]), body, context.userId));
      return true;
    }

    if (taskNotesMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listTaskNotes(Number(taskNotesMatch[1]), context.userId));
      return true;
    }

    const projectNotesMatch = pathname.match(/^\/projects\/(\d+)\/notes$/);
    if (projectNotesMatch && method === "POST") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const body = await readJsonBody<NotePayload>(request);
      sendJson(response, 201, await service.createProjectNote(Number(projectNotesMatch[1]), body, context.userId));
      return true;
    }

    if (projectNotesMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listProjectNotes(Number(projectNotesMatch[1]), context.userId));
      return true;
    }

    const deleteTaskNoteMatch = pathname.match(/^\/notes\/task\/(\d+)$/);
    if (deleteTaskNoteMatch && method === "DELETE") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      await service.deleteTaskNote(Number(deleteTaskNoteMatch[1]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    const deleteProjectNoteMatch = pathname.match(/^\/notes\/project\/(\d+)$/);
    if (deleteProjectNoteMatch && method === "DELETE") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      await service.deleteProjectNote(Number(deleteProjectNoteMatch[1]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    const projectNoteDeleteAliasMatch = pathname.match(/^\/projects\/(\d+)\/notes\/(\d+)$/);
    if (projectNoteDeleteAliasMatch && method === "DELETE") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      await service.deleteProjectNote(Number(projectNoteDeleteAliasMatch[2]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    const updateTaskNoteMatch = pathname.match(/^\/notes\/task\/(\d+)$/);
    if (updateTaskNoteMatch && method === "PUT") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const body = await readJsonBody<NotePayload>(request);
      sendJson(response, 200, await service.updateTaskNote(Number(updateTaskNoteMatch[1]), body, context.userId));
      return true;
    }

    const updateProjectNoteMatch = pathname.match(/^\/notes\/project\/(\d+)$/);
    if (updateProjectNoteMatch && method === "PUT") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const body = await readJsonBody<NotePayload>(request);
      sendJson(response, 200, await service.updateProjectNote(Number(updateProjectNoteMatch[1]), body, context.userId));
      return true;
    }

    if (method === "POST" && pathname === "/task/task-categories") {
      const context = auth();
      requireAdmin(context);
      const body = await readJsonBody<SimpleNamePayload>(request);
      sendJson(response, 201, await service.createTaskCategory(body.name ?? body.categoryName ?? "", context.userId));
      return true;
    }

    if (method === "GET" && pathname === "/task/task-categories") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listTaskCategories());
      return true;
    }

    if (method === "GET" && pathname === "/task-categories") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listTaskCategories());
      return true;
    }

    const taskCategoryDeleteMatch = pathname.match(/^\/task\/task-categories\/(\d+)$/);
    if (taskCategoryDeleteMatch && method === "DELETE") {
      const context = auth();
      requireAdmin(context);
      await service.deleteTaskCategory(Number(taskCategoryDeleteMatch[1]));
      response.writeHead(204);
      response.end();
      return true;
    }

    if (method === "POST" && pathname === "/status") {
      const context = auth();
      requireAdmin(context);
      const body = await readJsonBody<TaskStagePayload>(request);
      sendJson(response, 201, await service.createTaskStage(body, context.userId));
      return true;
    }

    if (method === "GET" && pathname === "/status") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listTaskStages());
      return true;
    }

    const statusByIdMatch = pathname.match(/^\/status\/(\d+)$/);
    if (statusByIdMatch && method === "PUT") {
      const context = auth();
      requireAdmin(context);
      const body = await readJsonBody<TaskStagePayload>(request);
      sendJson(response, 200, await service.updateTaskStage(Number(statusByIdMatch[1]), body));
      return true;
    }

    if (statusByIdMatch && method === "DELETE") {
      const context = auth();
      requireAdmin(context);
      await service.deleteTaskStage(Number(statusByIdMatch[1]));
      response.writeHead(204);
      response.end();
      return true;
    }

    const statusByProjectMatch = pathname.match(/^\/status\/project\/(\d+)$/);
    if (statusByProjectMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listTaskStagesForProject(Number(statusByProjectMatch[1])));
      return true;
    }

    if (method === "POST" && pathname === "/api/labels") {
      const context = auth();
      requireAdmin(context);
      const body = await readJsonBody<LabelPayload>(request);
      sendJson(response, 201, await service.createLabel(body, context.userId));
      return true;
    }

    if ((method === "GET" && pathname === "/api/labels") || (method === "GET" && pathname === "/api/labels/All")) {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listLabels());
      return true;
    }

    const labelByIdMatch = pathname.match(/^\/api\/labels\/(\d+)$/);
    if (labelByIdMatch && method === "PUT") {
      const context = auth();
      requireAdmin(context);
      const body = await readJsonBody<LabelPayload>(request);
      sendJson(response, 200, await service.updateLabel(Number(labelByIdMatch[1]), body));
      return true;
    }

    if (labelByIdMatch && method === "DELETE") {
      const context = auth();
      requireAdmin(context);
      await service.deleteLabel(Number(labelByIdMatch[1]));
      response.writeHead(204);
      response.end();
      return true;
    }

    const projectLabelsMatch = pathname.match(/^\/projects\/(\d+)\/labels$/);
    if (projectLabelsMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listLabels(Number(projectLabelsMatch[1])));
      return true;
    }

    if (method === "POST" && pathname === "/api/projects/tasks") {
      const context = auth();
      requireAdmin(context);
      const { payload, taskFile } = await readTaskMultipartPayload(request);
      sendJson(response, 201, await service.createTask(payload, context.userId, taskFile, true));
      return true;
    }

    if (method === "POST" && pathname === "/projects/tasks") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const { payload, taskFile } = await readTaskMultipartPayload(request);
      sendJson(response, 201, await service.createTask(payload, context.userId, taskFile, false));
      return true;
    }

    if (method === "GET" && pathname === "/api/projects/tasks/getAll") {
      const context = auth();
      requireAdmin(context);
      sendJson(response, 200, await service.listAllTasks(context.userId));
      return true;
    }

    if (method === "GET" && pathname === "/api/projects/tasks/waiting") {
      const context = auth();
      requireAdmin(context);
      sendJson(response, 200, await service.listWaitingTasks(context.userId));
      return true;
    }

    const adminTaskByIdMatch = pathname.match(/^\/api\/projects\/tasks\/(\d+)$/);
    if (adminTaskByIdMatch && method === "PUT") {
      const context = auth();
      requireAdmin(context);
      const { payload, taskFile } = await readTaskMultipartPayload(request);
      sendJson(response, 200, await service.updateTask(Number(adminTaskByIdMatch[1]), payload, context.userId, taskFile));
      return true;
    }

    if (adminTaskByIdMatch && method === "DELETE") {
      const context = auth();
      requireAdmin(context);
      await service.deleteTask(Number(adminTaskByIdMatch[1]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    const adminTaskStatusMatch = pathname.match(/^\/api\/projects\/tasks\/(\d+)\/status$/);
    if (adminTaskStatusMatch && method === "PATCH") {
      const context = auth();
      requireAdmin(context);
      const statusId = url.searchParams.get("statusId");
      if (!statusId) {
        throw new HttpError(400, "statusId query param is required");
      }
      sendJson(response, 200, await service.changeTaskStatus(Number(adminTaskStatusMatch[1]), Number(statusId), context.userId));
      return true;
    }

    const adminTaskDuplicateMatch = pathname.match(/^\/api\/projects\/tasks\/(\d+)\/duplicate$/);
    if (adminTaskDuplicateMatch && method === "POST") {
      const context = auth();
      requireAdmin(context);
      sendJson(response, 201, await service.duplicateTask(Number(adminTaskDuplicateMatch[1]), context.userId));
      return true;
    }

    const adminTaskApproveMatch = pathname.match(/^\/api\/projects\/tasks\/(\d+)\/approve$/);
    if (adminTaskApproveMatch && method === "POST") {
      const context = auth();
      requireAdmin(context);
      sendJson(response, 200, await service.approveTask(Number(adminTaskApproveMatch[1]), context.userId));
      return true;
    }

    const adminTaskCopyLinksMatch = pathname.match(/^\/api\/projects\/tasks\/(\d+)\/copy-links$/);
    if (adminTaskCopyLinksMatch && method === "POST") {
      const context = auth();
      requireAdmin(context);
      sendJson(response, 200, await service.getTaskCopyLinks(Number(adminTaskCopyLinksMatch[1])));
      return true;
    }

    const tasksByEmployeeMatch = pathname.match(/^\/api\/projects\/tasks\/employee\/([^/]+)$/);
    if (tasksByEmployeeMatch && method === "GET") {
      const context = auth();
      requireAdmin(context);
      sendJson(
        response,
        200,
        await service.listAssignedTasks(decodeURIComponent(tasksByEmployeeMatch[1]), context.userId)
      );
      return true;
    }

    const taskCountByEmployeeMatch = pathname.match(/^\/api\/projects\/tasks\/employee\/([^/]+)\/stats\/count$/);
    if (taskCountByEmployeeMatch && method === "GET") {
      const context = auth();
      requireAdmin(context);
      sendJson(response, 200, await service.getAssignedTaskCount(decodeURIComponent(taskCountByEmployeeMatch[1])));
      return true;
    }

    if (method === "GET" && pathname === "/api/projects/tasks/status/counts") {
      const context = auth();
      requireAdmin(context);
      sendJson(response, 200, await service.getAllTaskCounts());
      return true;
    }

    const projectTaskByIdMatch = pathname.match(/^\/projects\/(\d+)\/tasks\/(\d+)$/);
    if (projectTaskByIdMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(
        response,
        200,
        await service.getTask(Number(projectTaskByIdMatch[1]), Number(projectTaskByIdMatch[2]), context.userId)
      );
      return true;
    }

    const deleteProjectTaskAliasMatch = pathname.match(/^\/api\/projects\/(\d+)\/tasks\/(\d+)$/);
    if (deleteProjectTaskAliasMatch && method === "DELETE") {
      const context = auth();
      requireAdmin(context);
      await service.deleteTask(Number(deleteProjectTaskAliasMatch[2]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    const taskByIdMatch = pathname.match(/^\/projects\/tasks\/(\d+)$/);
    if (taskByIdMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.getTaskById(Number(taskByIdMatch[1]), context.userId));
      return true;
    }

    const tasksByProjectMatch = pathname.match(/^\/projects\/(\d+)\/tasks$/);
    if (tasksByProjectMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listTasksByProject(Number(tasksByProjectMatch[1]), context.userId));
      return true;
    }

    if (method === "GET" && pathname === "/projects/tasks/counts/me") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.getTaskCountsForEmployee(context.userId));
      return true;
    }

    const subtasksMatch = pathname.match(/^\/tasks\/(\d+)\/subtasks$/);
    if (subtasksMatch && method === "POST") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const body = await readJsonBody<SubtaskPayload>(request);
      sendJson(response, 201, await service.createSubtask(Number(subtasksMatch[1]), body, context.userId));
      return true;
    }

    if (subtasksMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listSubtasks(Number(subtasksMatch[1])));
      return true;
    }

    const subtaskByIdMatch = pathname.match(/^\/tasks\/(\d+)\/subtasks\/(\d+)$/);
    if (subtaskByIdMatch && method === "PUT") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const body = await readJsonBody<SubtaskPayload>(request);
      sendJson(response, 200, await service.updateSubtask(Number(subtaskByIdMatch[2]), body, context.userId));
      return true;
    }

    if (subtaskByIdMatch && method === "DELETE") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      await service.deleteSubtask(Number(subtaskByIdMatch[2]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    if (subtaskByIdMatch && method === "PATCH") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.toggleSubtask(Number(subtaskByIdMatch[2]), context.userId));
      return true;
    }
  } catch (error) {
    handleError(response, error);
    return true;
  }

  return false;
}

function handleError(response: ServerResponse, error: unknown) {
  if (error instanceof HttpError) {
    sendJson(response, error.statusCode, error.payload ?? { message: error.message });
    return;
  }

  sendJson(response, 500, {
    message: error instanceof Error ? error.message : "Internal server error"
  });
}

async function readProjectMultipartPayload(request: IncomingMessage) {
  const multipart = await parseMultipartFormData(request);
  return {
    payload: {
      shortCode: getField(multipart.fields, "shortCode"),
      name: getField(multipart.fields, "name"),
      projectName: getField(multipart.fields, "projectName"),
      startDate: getField(multipart.fields, "startDate"),
      deadline: getField(multipart.fields, "deadline"),
      noDeadline: getField(multipart.fields, "noDeadline"),
      category: getField(multipart.fields, "category"),
      projectCategory: getField(multipart.fields, "projectCategory"),
      department: getField(multipart.fields, "department"),
      departmentId: getField(multipart.fields, "departmentId"),
      clientId: getField(multipart.fields, "clientId"),
      summary: getField(multipart.fields, "summary"),
      projectSummary: getField(multipart.fields, "projectSummary"),
      tasksNeedAdminApproval: getField(multipart.fields, "tasksNeedAdminApproval"),
      currency: getField(multipart.fields, "currency"),
      budget: getField(multipart.fields, "budget"),
      projectBudget: getField(multipart.fields, "projectBudget"),
      hoursEstimate: getField(multipart.fields, "hoursEstimate"),
      allowManualTimeLogs: getField(multipart.fields, "allowManualTimeLogs"),
      assignedEmployeeIds: getFlexibleField(multipart.fields, "assignedEmployeeIds"),
      projectStatus: getField(multipart.fields, "projectStatus"),
      progressPercent: getField(multipart.fields, "progressPercent"),
      calculateProgressThroughTasks: getField(multipart.fields, "calculateProgressThroughTasks")
    } satisfies ProjectPayload,
    companyFile: firstFile(multipart.files, "companyFile")
  };
}

async function readProjectUpdatePayload(request: IncomingMessage) {
  if (!isMultipartRequest(request)) {
    return {
      payload: await readJsonBody<ProjectPayload>(request),
      companyFile: null
    };
  }

  return readProjectMultipartPayload(request);
}

async function readTaskMultipartPayload(request: IncomingMessage) {
  if (!isMultipartRequest(request)) {
    return {
      payload: await readJsonBody<TaskPayload>(request),
      taskFile: null
    };
  }

  const multipart = await parseMultipartFormData(request);
  return {
    payload: {
      projectId: getField(multipart.fields, "projectId"),
      title: getField(multipart.fields, "title"),
      category: getField(multipart.fields, "category"),
      startDate: getField(multipart.fields, "startDate"),
      dueDate: getField(multipart.fields, "dueDate"),
      noDueDate: getField(multipart.fields, "noDueDate"),
      taskStageId: getField(multipart.fields, "taskStageId"),
      assignedEmployeeIds: getFlexibleField(multipart.fields, "assignedEmployeeIds"),
      description: getField(multipart.fields, "description"),
      labelIds: getFlexibleField(multipart.fields, "labelIds"),
      milestoneId: getField(multipart.fields, "milestoneId"),
      priority: getField(multipart.fields, "priority"),
      isPrivate: getField(multipart.fields, "isPrivate"),
      timeEstimate: getField(multipart.fields, "timeEstimate"),
      timeEstimateMinutes: getField(multipart.fields, "timeEstimateMinutes"),
      isDependent: getField(multipart.fields, "isDependent"),
      dependentTaskId: getField(multipart.fields, "dependentTaskId")
    } satisfies TaskPayload,
    taskFile: firstFile(multipart.files, "taskFile", "file")
  };
}

function getField(fields: Record<string, string[]>, name: string) {
  const values = fields[name];
  if (!values?.length) {
    return undefined;
  }

  const value = values[values.length - 1]?.trim();
  return value === "" ? undefined : value;
}

function getFlexibleField(fields: Record<string, string[]>, name: string) {
  const values = fields[name]?.map((value) => value.trim()).filter(Boolean);
  if (!values?.length) {
    return undefined;
  }
  return values.length === 1 ? values[0] : values;
}

function normalizeStringArray(value: string[] | string | null | undefined): string[] {
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

function firstFile(files: Record<string, MultipartFieldFile[]>, ...names: string[]) {
  for (const name of names) {
    const file = files[name]?.[0];
    if (file) {
      return file;
    }
  }

  return null;
}

function isMultipartRequest(request: IncomingMessage) {
  return (request.headers["content-type"] ?? "").includes("multipart/form-data");
}

function isJsonRequest(request: IncomingMessage) {
  return (request.headers["content-type"] ?? "").includes("application/json");
}

function requireSelfOrAdmin(context: AuthContext, employeeId: string) {
  if (context.role !== "ROLE_ADMIN" && context.userId !== employeeId) {
    throw new HttpError(403, "You can only access your own timesheets");
  }
}
