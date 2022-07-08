//
//  BeneficiaryDelegate.swift
//  HelloCordova
//
//  Created by Luis Bouça on 31/05/2022.
//

import Foundation
import RemittancesSDK

class BenificiaryDelegateImp: BeneficiaryDelegate{
    
    var callbackId:String!
    var commandDelegate:CDVCommandDelegate!
    
    func createBeneficiary(token:String,accountUuid:String,data:CreateBeneficiaryRequest,commandDelegate:CDVCommandDelegate,callbackId:String){
        self.callbackId = callbackId
        self.commandDelegate = commandDelegate
        AlRemittances.shared.createBeneficiary(token: token, accountUuid: accountUuid, beneficiaryData: data, delegate: self)
    }
    
    func didCreateBeneficiary(_ beneficiary: Beneficiary) {
        handleEvent(message: BenificiaryToJson(beneficiary: beneficiary))
    }
    
    func getBeneficiary(token:String,accountUuid:String,benificiaryUuid:String,commandDelegate:CDVCommandDelegate,callbackId:String){
        self.callbackId = callbackId
        self.commandDelegate = commandDelegate
        AlRemittances.shared.getBeneficiary(token: token, accountUuid: accountUuid, beneficiaryUuid: benificiaryUuid, delegate: self)
    }
    
    func didGetBeneficiary(_ beneficiary: Beneficiary) {
        handleEvent(message: BenificiaryToJson(beneficiary: beneficiary))
    }
    
    func getBeneficiaries(token:String,accountUuid:String,commandDelegate:CDVCommandDelegate,callbackId:String){
        self.callbackId = callbackId
        self.commandDelegate = commandDelegate
        AlRemittances.shared.getBeneficiaries(token: token, accountUuid: accountUuid, delegate: self)
    }
    
    func didGetBeneficiaries(_ beneficiaries: [Beneficiary]) {
        let jsonData = try! JSONSerialization.data(withJSONObject: beneficiaries, options: JSONSerialization.WritingOptions.prettyPrinted)
        let result = String(data: jsonData, encoding: String.Encoding.utf8) ?? "{}"
        handleEvent(message: result)
    }
    
    func updateBeneficiary(token: String,accountUuid: String, beneficiaryUuid: String, beneficiaryData: UpdateBeneficiaryRequest, commandDelegate: CDVCommandDelegate, callbackId: String){
        self.callbackId = callbackId
        self.commandDelegate = commandDelegate
        AlRemittances.shared.updateBeneficiary(token: token,accountUuid: accountUuid, beneficiaryUuid: beneficiaryUuid, beneficiaryData: beneficiaryData, delegate: self)
    }
    
    func didUpdateBeneficiary(_ beneficiary: Beneficiary) {
        handleEvent(message: BenificiaryToJson(beneficiary: beneficiary))
    }
    
    func deleteBeneficiary(token: String, accountUuid: String, beneficiaryUuid: String, commandDelegate: CDVCommandDelegate, callbackId: String){
        self.callbackId = callbackId
        self.commandDelegate = commandDelegate
        AlRemittances.shared.deleteBeneficiary(token: token,accountUuid: accountUuid, beneficiaryUuid: beneficiaryUuid, delegate: self)
    }
    
    func didDeleteBeneficiary() {
        handleEvent(message: "Deleted Benificiary!")
    }
    
    
    func BenificiaryToJson(beneficiary:Beneficiary) -> String{
        let jsonData = try! JSONSerialization.data(withJSONObject: beneficiary, options: JSONSerialization.WritingOptions.prettyPrinted)
        let result = String(data: jsonData, encoding: String.Encoding.utf8) ?? "{}"
        return result;
    }
    
    
    func didHandleEvent(_ event: String, metadata: [String : String]?) {
        let jsonData = try! JSONSerialization.data(withJSONObject: metadata ?? [:], options: JSONSerialization.WritingOptions.prettyPrinted)
        let jsonMetadata = String(data: jsonData, encoding: String.Encoding.utf8)
        let result = CDVPluginResult.init(status: CDVCommandStatus.error, messageAs: "{\"event\":\"\(event)\" metadata: \(jsonMetadata ?? "[]")")
        commandDelegate.send(result, callbackId: callbackId)
    }
    
    func handleEvent(message:String){
        let result = CDVPluginResult.init(status: CDVCommandStatus.ok, messageAs: message)
        commandDelegate.send(result, callbackId: callbackId)
    }
}
