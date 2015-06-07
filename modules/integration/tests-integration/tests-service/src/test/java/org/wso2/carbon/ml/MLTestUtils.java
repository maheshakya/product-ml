/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.ml;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.ml.integration.common.utils.MLBaseTest;
import org.wso2.carbon.ml.integration.common.utils.MLHttpClient;
import org.wso2.carbon.ml.integration.common.utils.MLIntegrationTestConstants;
import org.wso2.carbon.ml.integration.common.utils.exception.MLHttpClientException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 *  This class contains the utility methods required to create tests
 */
public class MLTestUtils extends MLBaseTest {

    private static String analysisName;
    private static String modelName;
    private static int analysisId;
    private static int modelId;

    /**
     *
     * @param modelName         Name of the built model
     * @return status           Whether status of the model is complete or not.
     * @throws MLHttpClientException
     * @throws JSONException
     * @throws IOException
     */
    public static boolean checkModelStatus(String modelName, MLHttpClient mlHttpclient) throws MLHttpClientException, JSONException, IOException {
        CloseableHttpResponse response = mlHttpclient.doHttpGet("/api/models/" + modelName);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        JSONObject responseJson = new JSONObject(bufferedReader.readLine());
        bufferedReader.close();
        response.close();

        // Checks whether status is equal to Complete.
        boolean status = responseJson.getString("status").equals("Complete");
        return status;
    }

    /**
     * Sets the configuration of the model to be trained
     *
     * @param algorithmName     Name of the learning algorithm
     * @param algorithmType     Type of the learning algorithm
     * @param response          Response attribute
     * @param trainDataFraction Fraction of data from the dataset to be trained with
     * @param projectID         ID of the project
     * @param datasetID     Additional information about the name
     * @throws MLHttpClientException
     */
    public static String setConfiguration(String algorithmName, String algorithmType, String response,
                                  String trainDataFraction, int projectID, int datasetID, MLHttpClient mlHttpclient) throws MLHttpClientException, IOException, JSONException {
        analysisName = algorithmName + datasetID;

        //Create an analysis
        mlHttpclient.createAnalysis(analysisName, projectID);
        analysisId = mlHttpclient.getAnalysisId(analysisName);
        mlHttpclient.setFeatureDefaults(analysisId);

        //Set Model Configurations
        Map<String, String> configurations = new HashMap<String, String>();
        configurations.put(MLIntegrationTestConstants.ALGORITHM_NAME, algorithmName);
        configurations.put(MLIntegrationTestConstants.ALGORITHM_TYPE, algorithmType);
        configurations.put(MLIntegrationTestConstants.RESPONSE, response);
        configurations.put(MLIntegrationTestConstants.TRAIN_DATA_FRACTION_CONFIG, trainDataFraction);
        mlHttpclient.setModelConfiguration(analysisId, configurations);

        //Set default Hyper-parameters
        mlHttpclient.doHttpPost("/api/analyses/" + analysisId + "/hyperParams/defaults", null);

        // Create a model
        CloseableHttpResponse httpResponse = mlHttpclient.createModel(analysisId, mlHttpclient.getAVersionSetIdOfDataset(datasetID));
        modelName = mlHttpclient.getModelName(httpResponse);
        modelId = mlHttpclient.getModelId(modelName);

        //Set storage location for model
        mlHttpclient.createFileModelStorage(modelId, getModelStorageDirectory());

        return modelName;
    }
}