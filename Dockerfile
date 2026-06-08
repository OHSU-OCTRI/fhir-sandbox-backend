# fhir-sandbox-backend
#
# docker build -t ghcr.io/ohsu-octri/fhir-sandbox-backend --rm=true --pull .

FROM octri.ohsu.edu/jarrunner:17
EXPOSE 8080
COPY --chown=svcoctrikube:octrikube target/ROOT.war /app.jar
USER svcoctrikube