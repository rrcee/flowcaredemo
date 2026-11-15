import SwiftUI
import FlowCare

@main
struct FlowCareApp: App {
    var body: some Scene { WindowGroup { FlowCareView().ignoresSafeArea() } }
}
struct FlowCareView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController { MainViewControllerKt.MainViewController() }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
