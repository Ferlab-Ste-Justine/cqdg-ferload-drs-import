# CQDG Ferload DRS Import

Ferload DRS Import is an api that creates document resources in keycloak based on existing FHIR Documents. The Keycloak resources will be created based on the input study id provided and the study version in FHIR.


## Environment variables

Keyckloak Authentication server information :

- `KEYCLOAK_URL` : Keycloak URL
- `KEYCLOAK_REALM` : Keycloak Realm
- `KEYCLOAK_CLIENT_KEY` : Id of the client that contains resource definition and permissions
- `KEYCLOAK_CLIENT_SECRET` : Secret of the client that contains resource definition and permissions

AWS S3 information :

- `AWS_ACCESS_KEY` : Access key of the AWS account
- `AWS_SECRET_KEY` : Secret key of the AWS account
- `S3_CLINICAL_DATA_BUCKET_NAME` : Default bucket to use fir logging response
- `AWS_ENDPOINT`: Endpoint to S3 service. Can be empty.
- `AWS_PATH_ACCESS_STYLE` : Path access style to S3 service (true for minio, false for AWS). Default false.
- `AWS_REGION` : Region of the AWS account. Can be empty.

FHIR information :

- `FHIR_URL` : FHIR URL
  
FHIR information :

- `FERLOAD_URL` : Ferload URL