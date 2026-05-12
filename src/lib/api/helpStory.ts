import { apiRootBaseUrl, instance } from "./axios";
import type { HelpStoryPost, HelpStoryPostsListResponse } from "@/types/helpStory";

export const getPost = async (postId: string): Promise<HelpStoryPost> => {
  const response = await instance.get<HelpStoryPost>(`/posts/${postId}`, {
    baseURL: apiRootBaseUrl,
  });
  return response.data;
};

export const getPosts = async (): Promise<HelpStoryPostsListResponse> => {
  const response = await instance.get<HelpStoryPostsListResponse>("/posts", {
    baseURL: apiRootBaseUrl,
  });
  return response.data;
};

export type PostHelpStoryPayload = {
  title: string;
  content: string;
  images: File[];
};

export const postPosts = async (payload: PostHelpStoryPayload) => {
  const formData = new FormData();
  formData.append(
    "dto",
    new Blob([JSON.stringify({ title: payload.title, content: payload.content })], {
      type: "application/json",
    }),
  );
  for (const file of payload.images) {
    formData.append("images", file);
  }

  const response = await instance.post("/posts", formData, {
    baseURL: apiRootBaseUrl,
  });
  return response.data;
};

export const putPosts = async (postId: string, payload: PostHelpStoryPayload) => {
  const formData = new FormData();
  formData.append(
    "dto",
    new Blob([JSON.stringify({ title: payload.title, content: payload.content })], {
      type: "application/json",
    }),
  );
  for (const file of payload.images) {
    formData.append("images", file);
  }

  const response = await instance.put(`/posts/${postId}`, formData, {
    baseURL: apiRootBaseUrl,
  });
  return response.data;
};

export const deletePosts = async (postId: string) => {
  const response = await instance.delete(`/posts/${postId}`, {
    baseURL: apiRootBaseUrl,
  });
  return response.data;
};
