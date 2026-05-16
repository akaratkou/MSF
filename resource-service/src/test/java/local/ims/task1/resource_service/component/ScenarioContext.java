package local.ims.task1.resource_service.component;

import io.cucumber.spring.ScenarioScope;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scenario-scoped context: shares state between step definitions within one scenario.
 * A fresh instance is created for every scenario.
 */
@Component
@ScenarioScope
public class ScenarioContext {

    private byte[]                       requestBody;
    private MockHttpServletResponse      lastResponse;
    private Integer                      firstUploadId;
    private List<byte[]>                 multipleFiles      = new ArrayList<>();
    private List<MockHttpServletResponse> multipleResponses = new ArrayList<>();
    private String                       longCsvString;
    private final Map<Integer, Integer>  idMapping          = new HashMap<>();
    private List<Integer>                lastDeleteLabelIds = new ArrayList<>();

    public byte[]                    getRequestBody()                            { return requestBody; }
    public void                      setRequestBody(byte[] v)                     { requestBody = v; }

    public MockHttpServletResponse   getLastResponse()                           { return lastResponse; }
    public void                      setLastResponse(MockHttpServletResponse v)   { lastResponse = v; }

    public Integer  getFirstUploadId()              { return firstUploadId; }
    public void     setFirstUploadId(Integer v)      { firstUploadId = v; }

    public List<byte[]>                  getMultipleFiles()                           { return multipleFiles; }
    public void                          setMultipleFiles(List<byte[]> v)              { multipleFiles = v; }

    public List<MockHttpServletResponse> getMultipleResponses()                                    { return multipleResponses; }
    public void                          setMultipleResponses(List<MockHttpServletResponse> v)      { multipleResponses = v; }

    public String  getLongCsvString()               { return longCsvString; }
    public void    setLongCsvString(String v)        { longCsvString = v; }

    public Map<Integer, Integer> getIdMapping()     { return idMapping; }
    public void  mapId(int label, int actual)        { idMapping.put(label, actual); }
    public int   resolveId(int label)                { return idMapping.getOrDefault(label, label); }

    public List<Integer> getLastDeleteLabelIds()                  { return lastDeleteLabelIds; }
    public void          setLastDeleteLabelIds(List<Integer> v)    { lastDeleteLabelIds = v; }
}
