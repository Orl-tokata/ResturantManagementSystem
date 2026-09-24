import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

// Unmount between tests so one test's DOM cannot be found by the next.
afterEach(cleanup);
