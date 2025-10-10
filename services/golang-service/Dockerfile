# ---------- build stage ----------
FROM golang:1.22-alpine AS builder

# Build environment
ENV CGO_ENABLED=0 GOOS=linux GOARCH=amd64

WORKDIR /src

RUN apk add --no-cache git ca-certificates

COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN go build -trimpath -ldflags="-s -w" -o /src/server .

# ---------- runtime stage ----------
FROM alpine:3.18

RUN apk add --no-cache ca-certificates tzdata && update-ca-certificates

RUN addgroup -S app && adduser -S -G app app
WORKDIR /app

COPY --from=builder /src/server /app/server

RUN chown -R app:app /app
USER app

EXPOSE 8080

# Runtime environment
ENV PORT=8080
ENV GIN_MODE=release

CMD ["/app/server"]
