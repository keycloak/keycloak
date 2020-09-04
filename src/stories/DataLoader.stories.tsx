import React, { ReactNode } from "react";
import { Meta } from "@storybook/react";

import { DataLoader } from "../components/data-loader/DataLoader";

export default {
  title: "Data Loader",
  component: DataLoader,
} as Meta;

type Post = {
  title: string;
  body: string;
};

export const loadPosts = () => {
  const PostLoader = (props: { url: string; children: ReactNode }) => {
    const loader = async () => {
      const wait = (ms: number, value: Post) =>
        new Promise((resolve) => setTimeout(resolve, ms, value));
      return await fetch(props.url)
        .then((res) => res.json())
        .then((value) => wait(3000, value));
    };
    return <DataLoader loader={loader}>{props.children}</DataLoader>;
  };

  return (
    <PostLoader url="https://jsonplaceholder.typicode.com/posts">
      {(posts: Post[]) => (
        <table>
          <tr>
            <th>Name</th>
            <th>Description</th>
          </tr>
          {posts.map((post, i) => (
            <tr key={i}>
              <td>{post.title}</td>
              <td>{post.body}</td>
            </tr>
          ))}
        </table>
      )}
    </PostLoader>
  );
};
