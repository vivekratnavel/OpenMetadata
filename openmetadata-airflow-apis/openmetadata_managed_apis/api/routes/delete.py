#  Copyright 2021 Collate
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#  http://www.apache.org/licenses/LICENSE-2.0
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
"""
Delete the DAG in Airflow's db, as well as the python file
"""
import logging
import traceback

from airflow.api_connexion import security
from airflow.security import permissions
from airflow.www.app import csrf
from flask import Response
from openmetadata_managed_apis.api.app import blueprint
from openmetadata_managed_apis.api.response import ApiResponse
from openmetadata_managed_apis.api.utils import get_arg_dag_id
from openmetadata_managed_apis.operations.delete import delete_dag_id
from werkzeug.utils import secure_filename


@blueprint.route("/delete", methods=["DELETE"])
@csrf.exempt
@security.requires_access([(permissions.ACTION_CAN_DELETE, permissions.RESOURCE_DAG)])
def delete_dag() -> Response:
    """
    POST request to DELETE a DAG.

    Expect: POST
    {
        "workflow_name": "my_ingestion_pipeline3"
    }
    """
    dag_id = get_arg_dag_id()
    # Sanitize Path Traversal, as later on we clean files based on the DAG id
    secure_dag_id = secure_filename(dag_id)

    try:
        return delete_dag_id(secure_dag_id)

    except Exception as exc:
        logging.info(f"Failed to delete dag {dag_id} [secured: {secure_dag_id}]")
        return ApiResponse.error(
            status=ApiResponse.STATUS_SERVER_ERROR,
            error=f"Failed to delete {dag_id} [secured: {secure_dag_id}] due to {exc} - {traceback.format_exc()}",
        )
