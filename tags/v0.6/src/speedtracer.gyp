{
  'includes': [
    'third_party/build/common.gypi',
  ],
  'conditions' : [
    ['OS=="win"', {
      'targets': [
        {
          'target_name': 'All',
          'type': 'none',
          'dependencies': [
            'common/common.gyp:*',
            'client/plugin/plugin.gyp:*',
            'instruments/ie/ie_instruments.gyp:*',
          ],
        },
      ],
    }],
    ['OS=="mac"', {
      'targets': [
        {
          'target_name': 'All',
          'type': 'none',
          'dependencies': [
            # TODO(knorton): Add dependenices back as the are fixed for mac.
          ],
        },
      ],
    }],
  ],
}
