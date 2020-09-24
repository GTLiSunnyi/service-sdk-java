package iservice.sdk.util;

import com.alibaba.fastjson.JSON;
import iservice.sdk.entity.ServiceListenerOptions;
import iservice.sdk.entity.options.ConsumerListenerOptions;
import iservice.sdk.entity.options.ProviderListenerOptions;
import iservice.sdk.enums.SubscribeQueryKeyEnum;
import iservice.sdk.exception.ServiceSDKException;
import iservice.sdk.message.*;
import iservice.sdk.message.result.ResultEndBlock;
import iservice.sdk.message.result.ResultEvents;
import iservice.sdk.message.result.ServiceReqResult;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author : ori
 * @date : 2020/9/23 10:17 下午
 */
public class DecodeUtil {
    public static String decodeProviderReq(String json, ProviderListenerOptions options) {
        json = formatJson(json);
        ServiceReqMessage x = JSON.parseObject(json, ServiceReqMessage.class);
        ServiceReqResult result = x.getResult();
        if (!checkMessageType(result.getQuery(), options)) {
            return null;
        }
        ResultEndBlock resultEndBlock = result.getData().getValue().getResult_end_block();
        List<ResultEvents> events = filterEventsByKey(options.getListenerType().getParamPrefix(), resultEndBlock.getEvents());
        ResultEvents targetEvent = events.stream().filter(event -> {
            event.getDecodeAttributes();
            return event.compareAttribute(SubscribeQueryKeyEnum.PROVIDER.getKey(), options.getAddress())
                    && event.compareAttribute(SubscribeQueryKeyEnum.SERVICE_NAME.getKey(), options.getServiceName());
        }).findAny().orElse(null);
        if (targetEvent == null) {
            throw new ServiceSDKException("Listener info not found! serviceName='" + options.getServiceName() + "' providerAddress='" + options.getAddress() + "'");
        }
        String requestsJson = targetEvent.getAttributesValueByKey("requests");
        return JSON.parseArray(requestsJson, String.class).get(0);
    }

    private static boolean checkMessageType(String query, ServiceListenerOptions options) {
        return Objects.equals(query,SubscribeUtil.buildSubscribeParam(options).getQuery());
    }

    public static String decodeConsumerReq(String json, ConsumerListenerOptions options) {
        json = formatJson(json);
        ServiceResMessage message = JSON.parseObject(json, ServiceResMessage.class);
        ServiceResResult messageResult = message.getResult();
        if (!checkMessageType(messageResult.getQuery(), options)) {
            return null;
        }
        TxResultInfo result = messageResult.getData().getValue().get(TxResult.CLASS_NAME).getResult();
        List<ResultEvents> events = filterEventsByKey(options.getListenerType().getParamPrefix(), result.getEvents());
        ResultEvents targetEvent = events.stream().filter(event -> {
            event.getDecodeAttributes();
            return event.compareAttribute(SubscribeQueryKeyEnum.CONSUMER.getKey(), options.getAddress())
                    && event.compareAttribute(SubscribeQueryKeyEnum.SENDER.getKey(), options.getSender())
                    && event.compareAttribute(SubscribeQueryKeyEnum.MODULE.getKey(), options.getModule());
        }).findAny().orElse(null);
        if (targetEvent == null) {
            throw new ServiceSDKException("Listener info not found! ListenerOption=" + JSON.toJSONString(options));
        }
        return targetEvent.getAttributesValueByKey("request_id");

    }

    public static String formatJson(String json) {
//        String regex1 = "^.+?} *\\{.+?$";
//        if (!Pattern.matches(regex1, json)) {
//            return json;
//        }
        return json.replaceAll("}[ \\n]*\\{.+?$", "}");
    }

    private static List<ResultEvents> filterEventsByKey(String type, List<ResultEvents> events) {
        return events.stream()
                .filter(event -> event.equalsType(type))
                .collect(Collectors.toList());
    }
}