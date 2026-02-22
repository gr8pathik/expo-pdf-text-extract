require 'json'

package = JSON.parse(File.read(File.join(__dir__, '..', 'package.json')))

Pod::Spec.new do |s|
  s.name           = 'PdfExtractor'
  s.version        = package['version']
  s.summary        = package['description']
  s.description    = "#{package['description']} - Native PDF text extraction for iOS using PDFKit."
  s.license        = package['license']
  s.authors        = package['author']['name']
  s.homepage       = package['homepage']
  s.platforms      = {
    :ios => '15.1'
  }
  s.swift_version  = '5.9'
  s.source         = { git: package['repository']['url'].gsub('git+', '').gsub('.git', ''), tag: "v#{s.version}" }
  s.static_framework = true

  s.dependency 'ExpoModulesCore'

  # Swift/Objective-C compatibility
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'SWIFT_COMPILATION_MODE' => 'wholemodule'
  }

  s.source_files = "**/*.{h,m,swift}"
  s.exclude_files = "**/*Tests*.swift"
end
