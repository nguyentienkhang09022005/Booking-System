import mongoose from "mongoose";
import dotenv from "dotenv";
dotenv.config();
let connected = false;
export async function connectMongo() {
    if (connected)
        return;
    await mongoose.connect(process.env.SPRING_DATA_MONGODB_URI, {
        autoIndex: false
    });
    connected = true;
}
//# sourceMappingURL=mongo.js.map