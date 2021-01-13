import React from "react";

import "./work-in-progress.css";

type WorkInProgressProps = {
  marvelLink: string;
};

export const WorkInProgress = ({ marvelLink }: WorkInProgressProps) => (
  <div>
    This page is not completed yet, but this is what it's going to look like:
    <div className="container">
      <iframe className="responsive-iframe" src={marvelLink}></iframe>
    </div>
  </div>
);
