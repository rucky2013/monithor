<template>
  <div class="fluid-container">
    <vuetable ref="vuetable"
              :fields="columns"
              :append-params="queryParams"
              :sort-order="sortOrder"
              api-url="/jobs/"
              data-path=""
              pagination-path=""
              @vuetable:load-error="error"
    />
  </div>
</template>

<script>
  import Vue from 'vue'
  import Vuetable from 'vuetable-2/src/components/Vuetable.vue'
  import router from '../router'
  import utils from '../utils'

  Vue.component('row-actions', {
    template: [
      '<span>',
      '<button type="button" class="btn btn-default btn-sm" @click="onClick(\'edit\', rowData)"><span class="glyphicon glyphicon-pencil"></span></button> ',
      '<button type="button" class="btn btn-default btn-sm" @click="onClick(\'delete\', rowData)"><span class="glyphicon glyphicon-trash"></span></button>',
      '</span>'
    ].join(''),
    props: {
      rowData: {
        type: Object,
        required: true
      }
    },
    methods: {
      onClick (action, job) {
        if (action === 'delete') {
          if (window.confirm('Sure you want to delete the Job \'' + job.name + '\'?')) {
            this.$http.delete('/jobs/' + job.id).then((response) => {
              this.$parent.reload()
            }, (response) => {
              this.$parent.$parent.error(response)
            })
          }
        }
        if (action === 'edit') {
          router.push({name: 'edit_job', params: {id: job.id}})
        }
      }
    }
  })

  export default {
    name: 'jobs',
    props: {
      bus: {
        type: Object,
        required: true
      }
    },
    components: {
      Vuetable
    },
    data () {
      return {
        columns: [
          {name: 'lastResult', title: 'Status', callback: 'status'},
          'name',
          {name: 'url', title: 'URL', callback: 'url'},
          {name: 'tags', title: 'Tags', callback: 'tags'},
          {name: 'lastVersion', title: 'Version'},
          {name: 'lastDuration', title: 'Duration'},
          {name: 'lastTimestamp', title: 'Last check', callback: 'ago'},
          '__component:row-actions'
        ],
        queryParams: {
          queryString: ''
        },
        sortOrder: [{
          field: 'lastTimestamp',
          direction: 'desc'
        }]
      }
    },
    methods: {
      reload () {
        this.$refs.vuetable.reload()
      },
      status (status) {
        if (status === true) {
          return '<span class="label label-success label" style="font-size:larger">Success</span>'
        } else if (status === false) {
          return '<span class="label label-danger label" style="font-size:larger">Failure</span>'
        } else {
          return '<span class="label label-default label" style="font-size:larger">Unknown</span>'
        }
      },
      ago (timestamp) {
        return utils.timeAgo(timestamp)
      },
      url (url) {
        return '<a href=' + url + '>' + url + '</a>'
      },
      tags (tags) {
        if (tags) {
          return tags.map((tag) => { return '<span class="label label-info">' + tag + '</span>' }).join(' ')
        }
      },
      error (response) {
        this.bus.$emit('error', response.statusText, response.status)
      }
    },
    mounted () {
      this.bus.$off('query')
      this.bus.$on('query', function (query) {
        this.queryParams.queryString = query
        this.$refs.vuetable.reload()
      }.bind(this))
    }
  }

</script>
<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>