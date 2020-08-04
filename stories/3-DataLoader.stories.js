import React from 'react';
import { storiesOf } from '@storybook/react';

import { DataLoader } from '../src/components/data-loader/DataLoader';

storiesOf('DataLoader', module)
  .add('load posts', () => {

    function PostLoader(props) {
      const loader = async () => {
        const wait = (ms, value) => new Promise(resolve => setTimeout(resolve, ms, value))
        return await fetch(props.url).then(res => res.json()).then(value => wait(3000, value));
      }
      return <DataLoader loader={loader}>{props.children}</DataLoader>;
    }

    return (
      <PostLoader url="https://jsonplaceholder.typicode.com/posts">
        {posts => (
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
  });