/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.cdo.avro.protocol;

@SuppressWarnings("all")
/** avro cdo rpc  */
@org.apache.avro.specific.AvroGenerated
public interface CDOProtocol {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"CDOProtocol\",\"namespace\":\"com.cdo.avro.protocol\",\"doc\":\"avro cdo rpc \",\"types\":[{\"type\":\"record\",\"name\":\"AvroCDO\",\"fields\":[{\"name\":\"level\",\"type\":\"int\"},{\"name\":\"fields\",\"type\":{\"type\":\"map\",\"values\":\"bytes\"}}]}],\"messages\":{\"send\":{\"request\":[{\"name\":\"avroCDOReq\",\"type\":\"AvroCDO\"}],\"response\":\"AvroCDO\"}}}");
  /**
   */
  com.cdo.avro.protocol.AvroCDO send(com.cdo.avro.protocol.AvroCDO avroCDOReq) throws org.apache.avro.AvroRemoteException;

  @SuppressWarnings("all")
  /** avro cdo rpc  */
  public interface Callback extends CDOProtocol {
    public static final org.apache.avro.Protocol PROTOCOL = com.cdo.avro.protocol.CDOProtocol.PROTOCOL;
    /**
     * @throws java.io.IOException The async call could not be completed.
     */
    void send(com.cdo.avro.protocol.AvroCDO avroCDOReq, org.apache.avro.ipc.Callback<com.cdo.avro.protocol.AvroCDO> callback) throws java.io.IOException;
  }
}