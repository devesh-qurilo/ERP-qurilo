import type { IncomingMessage, ServerResponse } from "node:http";
import { URL } from "node:url";

import { getAuthContext, requireAdmin, requireEmployeeOrAdmin } from "../../common/auth.js";
import { HttpError } from "../../common/errors.js";
import { parseMultipartFormData, readJsonBody, sendJson, type MultipartFieldFile } from "../../common/http.js";
import type { ProjectConfig } from "../../config/env.js";
import type {
  DiscussionCategoryPayload,
  DiscussionMessagePayload,
  DiscussionRoomPayload,
  ProjectService
} from "../../services/project.service.js";

interface SimpleDeletePayload {
  categoryName?: string;
  colorCode?: string | null;
}

interface MessageUpdatePayload {
  content?: string | null;
}

export async function handleDiscussionRoutes(
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
    if (method === "GET" && pathname === "/api/projects/discussion-categories") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listDiscussionCategories());
      return true;
    }

    if (method === "POST" && pathname === "/api/projects/discussion-categories") {
      const context = auth();
      requireAdmin(context);
      const body = await readJsonBody<SimpleDeletePayload>(request);
      sendJson(
        response,
        201,
        await service.createDiscussionCategory({
          categoryName: body.categoryName,
          colorCode: body.colorCode
        } satisfies DiscussionCategoryPayload)
      );
      return true;
    }

    const discussionCategoryByIdMatch = pathname.match(/^\/api\/projects\/discussion-categories\/(\d+)$/);
    if (discussionCategoryByIdMatch && method === "DELETE") {
      const context = auth();
      requireAdmin(context);
      await service.deleteDiscussionCategory(Number(discussionCategoryByIdMatch[1]));
      response.writeHead(204);
      response.end();
      return true;
    }

    const discussionRoomsMatch = pathname.match(/^\/api\/projects\/(\d+)\/discussion-rooms$/);
    if (discussionRoomsMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listDiscussionRooms(Number(discussionRoomsMatch[1])));
      return true;
    }

    if (discussionRoomsMatch && method === "POST") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const { payload, initialFile } = await readDiscussionRoomPayload(request);
      sendJson(
        response,
        201,
        await service.createDiscussionRoom(Number(discussionRoomsMatch[1]), payload, context.userId, initialFile)
      );
      return true;
    }

    const discussionRoomByIdMatch = pathname.match(/^\/api\/projects\/(\d+)\/discussion-rooms\/(\d+)$/);
    if (discussionRoomByIdMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.getDiscussionRoom(Number(discussionRoomByIdMatch[2])));
      return true;
    }

    if (discussionRoomByIdMatch && method === "DELETE") {
      const context = auth();
      requireAdmin(context);
      await service.deleteDiscussionRoom(Number(discussionRoomByIdMatch[2]));
      response.writeHead(204);
      response.end();
      return true;
    }

    const discussionMessagesMatch = pathname.match(/^\/api\/projects\/discussion-rooms\/(\d+)\/messages$/);
    if (discussionMessagesMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listDiscussionMessages(Number(discussionMessagesMatch[1])));
      return true;
    }

    if (discussionMessagesMatch && method === "POST") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const { payload, file } = await readDiscussionMessagePayload(request);
      sendJson(
        response,
        201,
        await service.createDiscussionMessage(Number(discussionMessagesMatch[1]), payload, context.userId, file)
      );
      return true;
    }

    const discussionFileMessageMatch = pathname.match(/^\/api\/projects\/discussion-rooms\/(\d+)\/messages\/file$/);
    if (discussionFileMessageMatch && method === "POST") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const multipart = await parseMultipartFormData(request);
      const file = firstFile(multipart.files, "file");
      if (!file) {
        throw new HttpError(400, "file is required");
      }
      sendJson(
        response,
        201,
        await service.createDiscussionMessage(
          Number(discussionFileMessageMatch[1]),
          { content: "", parentMessageId: null },
          context.userId,
          file
        )
      );
      return true;
    }

    const discussionMessageByIdMatch = pathname.match(/^\/api\/projects\/discussion-rooms\/(\d+)\/messages\/(\d+)$/);
    if (discussionMessageByIdMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.getDiscussionMessage(Number(discussionMessageByIdMatch[2])));
      return true;
    }

    if (discussionMessageByIdMatch && method === "PUT") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const body = await readJsonBody<MessageUpdatePayload | string>(request);
      const content = typeof body === "string" ? body : body.content ?? "";
      sendJson(response, 200, await service.updateDiscussionMessage(Number(discussionMessageByIdMatch[2]), content, context.userId));
      return true;
    }

    if (discussionMessageByIdMatch && method === "DELETE") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      await service.deleteDiscussionMessage(Number(discussionMessageByIdMatch[2]), context.userId);
      response.writeHead(204);
      response.end();
      return true;
    }

    const discussionRepliesMatch = pathname.match(/^\/api\/projects\/discussion-rooms\/(\d+)\/messages\/(\d+)\/replies$/);
    if (discussionRepliesMatch && method === "GET") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.listDiscussionReplies(Number(discussionRepliesMatch[2])));
      return true;
    }

    if (discussionRepliesMatch && method === "POST") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      const body = await readJsonBody<DiscussionMessagePayload>(request);
      sendJson(response, 201, await service.replyToDiscussionMessage(Number(discussionRepliesMatch[2]), body, context.userId));
      return true;
    }

    const discussionBestReplyMatch = pathname.match(/^\/api\/projects\/discussion-rooms\/(\d+)\/messages\/(\d+)\/mark-best-reply$/);
    if (discussionBestReplyMatch && method === "POST") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.markBestReply(Number(discussionBestReplyMatch[2])));
      return true;
    }

    const discussionUnmarkBestReplyMatch = pathname.match(/^\/api\/projects\/discussion-rooms\/(\d+)\/messages\/(\d+)\/unmark-best-reply$/);
    if (discussionUnmarkBestReplyMatch && method === "POST") {
      const context = auth();
      requireEmployeeOrAdmin(context);
      sendJson(response, 200, await service.unmarkBestReply(Number(discussionUnmarkBestReplyMatch[2])));
      return true;
    }
  } catch (error) {
    handleError(response, error);
    return true;
  }

  return false;
}

async function readDiscussionRoomPayload(request: IncomingMessage) {
  if (!isMultipartRequest(request)) {
    return {
      payload: await readJsonBody<DiscussionRoomPayload>(request),
      initialFile: null
    };
  }

  const multipart = await parseMultipartFormData(request);
  return {
    payload: {
      title: getField(multipart.fields, "title"),
      categoryId: getField(multipart.fields, "categoryId"),
      initialMessage: getField(multipart.fields, "initialMessage")
    } satisfies DiscussionRoomPayload,
    initialFile: firstFile(multipart.files, "initialFile", "file")
  };
}

async function readDiscussionMessagePayload(request: IncomingMessage) {
  if (!isMultipartRequest(request)) {
    return {
      payload: await readJsonBody<DiscussionMessagePayload>(request),
      file: null
    };
  }

  const multipart = await parseMultipartFormData(request);
  return {
    payload: {
      content: getField(multipart.fields, "content") ?? "",
      parentMessageId: getField(multipart.fields, "parentMessageId")
    } satisfies DiscussionMessagePayload,
    file: firstFile(multipart.files, "file")
  };
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

function firstFile(files: Record<string, MultipartFieldFile[]>, ...names: string[]) {
  for (const name of names) {
    const file = files[name]?.[0];
    if (file) {
      return file;
    }
  }

  return null;
}

function getField(fields: Record<string, string[]>, name: string) {
  const values = fields[name];
  if (!values?.length) {
    return undefined;
  }

  const value = values[values.length - 1]?.trim();
  return value === "" ? undefined : value;
}

function isMultipartRequest(request: IncomingMessage) {
  return (request.headers["content-type"] ?? "").includes("multipart/form-data");
}
