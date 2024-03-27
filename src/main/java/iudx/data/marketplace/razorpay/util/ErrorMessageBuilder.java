package iudx.data.marketplace.razorpay.util;

import iudx.data.marketplace.common.ResponseUrn;

import java.util.Map;

import static iudx.data.marketplace.apiserver.provider.linkedAccount.util.Constants.FAILURE_MESSAGE;

public class ErrorMessageBuilder {

  public static Map<String, String> initialiseMap() {
    return Map.ofEntries(
        Map.entry(
            "Merchant email already exists for account".toLowerCase(),
            FAILURE_MESSAGE + "merchant email already exists for account"),
        Map.entry(
            "The phone format is invalid".toLowerCase(),
            FAILURE_MESSAGE + "phone format is invalid"),
        Map.entry(
            "The contact name may only contain alphabets and spaces".toLowerCase(),
            FAILURE_MESSAGE + "name is invalid"),
        Map.entry(
            "Invalid business subcategory for business category".toLowerCase(),
            FAILURE_MESSAGE + "subcategory or category is invalid"),
        Map.entry(
            "The street2 field is required".toLowerCase(),
            FAILURE_MESSAGE + "street2 field is required"),
        Map.entry(
            "The street1 field is required".toLowerCase(),
            FAILURE_MESSAGE + "street1 field is required"),
        Map.entry(
            "The city field is required".toLowerCase(), FAILURE_MESSAGE + "city field is required"),
        Map.entry(
            "The business registered city may only contain alphabets, digits and spaces"
                .toLowerCase(),
            FAILURE_MESSAGE + "city name is invalid"),
        Map.entry(
            "State name entered is incorrect. Please provide correct state name".toLowerCase(),
            FAILURE_MESSAGE + "state name is invalid"),
        Map.entry(
            "The postal code must be an integer".toLowerCase(),
            FAILURE_MESSAGE + "postal code is invalid"),
        Map.entry(
            "The business registered country may only contain alphabets and spaces".toLowerCase(),
            FAILURE_MESSAGE + "country name is invalid"),
        Map.entry(
            "The pan field is invalid".toLowerCase(), FAILURE_MESSAGE + "pan field is invalid"),
        Map.entry(
            "The gst field is invalid".toLowerCase(), FAILURE_MESSAGE + "gst field is invalid"),
        Map.entry(
            "Route code Support feature not enabled to add account code".toLowerCase(),
            FAILURE_MESSAGE + "route code support feature not enabled to add account code"),
        Map.entry(
            "The api key/secret provided is invalid".toLowerCase(),
            FAILURE_MESSAGE + ResponseUrn.INTERNAL_SERVER_ERR_URN.getMessage()),
        Map.entry(
            "Merchant activation form has been locked for editing by admin".toLowerCase(),
            "Linked account updation failed as merchant activation form has been locked for editing by admin"));
  }
}
