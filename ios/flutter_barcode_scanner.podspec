#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint flutter_barcode_scanner.podspec' to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'flutter_barcode_scanner'
  s.version          = '0.0.1'
  s.summary          = 'A flutter barcode scanner.'
  s.description      = <<-DESC
A flutter barcode scanner.
                       DESC
  s.homepage         = 'https://github.com/JortdeBokx/flutter_barcode_scanner'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Jort de Bokx' => 'jort.debokx@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.dependency 'GoogleMLKit/BarcodeScanning'
  s.platform = :ios, '10.0'

  # Flutter.framework does not contain a i386 slice. Only x86_64 simulators are supported.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'VALID_ARCHS[sdk=iphonesimulator*]' => 'x86_64' }
  s.swift_version = '5.0'
end
