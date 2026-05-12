export interface HelpStoryPost {
  postId: string;
  title: string;
  content: string;
  writerName: string;
  viewCount: number;
  imageUrls: string[];
  /** `YYYY-MM-DD` */
  createdAt: string;
}

export interface HelpStoryPageSort {
  sorted: boolean;
  empty: boolean;
  unsorted: boolean;
}

export interface HelpStoryPageInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
  sort: HelpStoryPageSort;
}

export interface HelpStoryPostsListResponse {
  posts: HelpStoryPost[];
  page: HelpStoryPageInfo;
}
