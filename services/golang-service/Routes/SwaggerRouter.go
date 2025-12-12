package Routes

import (
	"fmt"
	"os"

	"github.com/gin-gonic/gin"
	swaggerFiles "github.com/swaggo/files"
	ginSwagger "github.com/swaggo/gin-swagger"
)

func SwaggerRouter(router *gin.Engine) {
	host := os.Getenv("SWAGGER_HOST")

	if host == "" {
		host = "localhost:8081"
	}

	scheme := os.Getenv("SWAGGER_SCHEME")
	if scheme == "" {
		// Railway dùng HTTPS – local thì HTTP
		if host == "localhost:8081" {
			scheme = "http"
		} else {
			scheme = "https"
		}
	}

	swaggerURL := ginSwagger.URL(fmt.Sprintf("%s://%s/swagger/doc.json", scheme, host))

	// Router Swagger
	router.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler, swaggerURL))
}
