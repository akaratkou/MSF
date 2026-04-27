#!/bin/bash

NAME=${BUCKET_NAME:-resource-bucket}

awslocal s3 mb "s3://$NAME"
echo "Bucket '$NAME' created!"