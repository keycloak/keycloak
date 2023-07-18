import styles from "@patternfly/react-styles/css/components/Avatar/avatar";
import { css } from "@patternfly/react-styles";

type DefaultAvatarProps = {
  className?: string;
  border?: "light" | "dark";
  size?: "sm" | "md" | "lg" | "xl";
};

export const DefaultAvatar = ({
  className = "",
  border,
  size = "md",
}: DefaultAvatarProps) => (
  <svg
    className={css(
      styles.avatar,
      styles.modifiers[size],
      border === "light" && styles.modifiers.light,
      border === "dark" && styles.modifiers.dark,
      className,
    )}
    enableBackground="new 0 0 36 36"
    version="1.1"
    viewBox="0 0 36 36"
    xmlns="http://www.w3.org/2000/svg"
  >
    <circle
      style={{ fillRule: "evenodd", clipRule: "evenodd", fill: "#FFFFFF" }}
      cx="18"
      cy="18.5"
      r="18"
    />
    <defs>
      <filter
        id="b"
        x="5.2"
        y="7.2"
        width="25.6"
        height="53.6"
        filterUnits="userSpaceOnUse"
      >
        <feColorMatrix values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 1 0" />
      </filter>
    </defs>
    <mask
      id="a"
      x="5.2"
      y="7.2"
      width="25.6"
      height="53.6"
      maskUnits="userSpaceOnUse"
    >
      <g style={{ filter: 'url("#b")' }}>
        <circle
          style={{ fillRule: "evenodd", clipRule: "evenodd", fill: "#FFFFFF" }}
          cx="18"
          cy="18.5"
          r="18"
        />
      </g>
    </mask>
    <g style={{ filter: 'url("#a")' }}>
      <g transform="translate(5.04 6.88)">
        <path
          style={{
            fillRule: "evenodd",
            clipRule: "evenodd",
            fill: "#BBBBBB",
          }}
          d="m22.6 18.1c-1.1-1.4-2.3-2.2-3.5-2.6s-1.8-0.6-6.3-0.6-6.1 0.7-6.1 0.7 0 0 0 0c-1.2 0.4-2.4 1.2-3.4 2.6-2.3 2.8-3.2 12.3-3.2 14.8 0 3.2 0.4 12.3 0.6 15.4 0 0-0.4 5.5 4 5.5l-0.3-6.3-0.4-3.5 0.2-0.9c0.9 0.4 3.6 1.2 8.6 1.2 5.3 0 8-0.9 8.8-1.3l0.2 1-0.2 3.6-0.3 6.3c3 0.1 3.7-3 3.8-4.4s0.6-12.6 0.6-16.5c0.1-2.6-0.8-12.1-3.1-15z"
        />
        <path
          style={{
            opacity: 0.1,
            fillRule: "evenodd",
            clipRule: "evenodd",
          }}
          d="m22.5 26c-0.1-2.1-1.5-2.8-4.8-2.8l2.2 9.6s1.8-1.7 3-1.8c0 0-0.4-4.6-0.4-5z"
        />
        <path
          style={{
            fillRule: "evenodd",
            clipRule: "evenodd",
            fill: "#BBBBBB",
          }}
          d="m12.7 13.2c-3.5 0-6.4-2.9-6.4-6.4s2.9-6.4 6.4-6.4 6.4 2.9 6.4 6.4-2.8 6.4-6.4 6.4z"
        />
        <path
          style={{
            opacity: 8.0e-2,
            fillRule: "evenodd",
            clipRule: "evenodd",
            fill: "#231F20",
          }}
          d="m9.4 6.8c0-3 2.1-5.5 4.9-6.3-0.5-0.1-1-0.2-1.6-0.2-3.5 0-6.4 2.9-6.4 6.4s2.9 6.4 6.4 6.4c0.6 0 1.1-0.1 1.6-0.2-2.8-0.6-4.9-3.1-4.9-6.1z"
        />
        <path
          style={{
            opacity: 0.1,
            fillRule: "evenodd",
            clipRule: "evenodd",
          }}
          d="m8.3 22.4c-2 0.4-2.9 1.4-3.1 3.5l-0.6 18.6s1.7 0.7 3.6 0.9l0.1-23z"
        />
      </g>
    </g>
  </svg>
);
