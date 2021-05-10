//
//  ViewController.swift
//  iOS-iTAG-Demo
//
//  Created by HuiWang on 2021/5/10.
//

import UIKit

class ViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
    }

    //开始报警
    @IBAction func startAlarm(_ sender: Any) {
        LLBlueTooth.instance.setBuzzerAlarm(isOn: true)
    }
    //停止报警
    @IBAction func stopAlarm(_ sender: Any) {
        LLBlueTooth.instance.setBuzzerAlarm(isOn: false)
    }
}

