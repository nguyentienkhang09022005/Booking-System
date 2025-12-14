import { neon } from "@neondatabase/serverless";
import dotenv from "dotenv";
dotenv.config();
const sql = neon(process.env.POSTGRESQL_CONNECTION);
export default sql;
//# sourceMappingURL=postgre.js.map