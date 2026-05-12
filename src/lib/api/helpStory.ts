import { instance } from "./axios";
import type { HelpStoryPost, HelpStoryPostsListResponse } from "@/types/helpStory";

export const getPost = async (postId: string): Promise<HelpStoryPost> => {
  const response = await instance.get<HelpStoryPost>(`/posts/${postId}`);
  return response.data;
};

export const getPosts = async (): Promise<HelpStoryPostsListResponse> => {
  const response = await instance.get<HelpStoryPostsListResponse>("/posts");
  return response.data;
};

export type PostHelpStoryPayload = {
  title: string;
  content: string;
  images: File[];
};

/** PUT /posts/:id — multipart `dto`(JSON) + `images`(파일) */
export type PutHelpStoryPayload = {
  postId: string;
  title: string;
  content: string;
  /** 서버에 그대로 둘 이미지 URL(새 파일로 교체하는 인덱스는 제외) */
  imageUrls: string[];
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

  const response = await instance.post("/posts", formData);
  return response.data;
};

export const putPosts = async (payload: PutHelpStoryPayload) => {
  const { postId, title, content, imageUrls, images } = payload;
  const formData = new FormData();
  formData.append(
    "dto",
    new Blob(
      [JSON.stringify({ postId, title, content, imageUrls })],
      { type: "application/json" },
    ),
  );
  for (const file of images) {
    formData.append("images", file);
  }

  const response = await instance.put(`/posts/${postId}`, formData);
  return response.data;
};

export const deletePosts = async (postId: string) => {
  const response = await instance.delete(`/posts/${postId}`);
  return response.data;
};
