import React from 'react'

import './info-view.css'

const InfoView = () => (
  <div>
    <p>
      需要提供部署openGauss数据库的服务器基本硬件信息。点击左下方生成按钮后计算推荐的配置结果。
    </p>
    <p className="info-view-db-type__description">关于数据库类型选项：</p>
    <ul className="info-view-db-type__list">
      <li>
        <div className="info-view-db-type__header">Web应用</div>
        <ul>
          <li>通常为计算密集型</li>
          <li>数据库远小于总内存</li>
          <li>简单查询占比90%以上</li>
        </ul>
      </li>
      <li>
        <div className="info-view-db-type__header">联机事务处理(oltp)</div>
        <ul>
          <li>计算密集型或IO密集型</li>
          {/*<li>DB slightly larger than RAM to 1TB</li>*/}
          <li>小数据写入占20%-40%</li>
          <li>存在一部分的长事务和复杂查询</li>
        </ul>
      </li>
      <li>
        <div className="info-view-db-type__header">数据仓库</div>
        <ul>
          <li>IO密集型或内存交换密集型</li>
          <li>大批量数据加载</li>
          <li>大量复杂的报告查询</li>
          <li>也被称为"决策支持"或"商业智能"</li>
        </ul>
      </li>
      <li>
        <div className="info-view-db-type__header">桌面应用</div>
        <ul>
          <li>非专用数据库</li>
          <li>通常为开发者所配置的开发环境</li>
        </ul>
      </li>
      <li>
        <div className="info-view-db-type__header">混合应用</div>
        <ul>
          <li>兼具数据仓库和联机事务处理的特征</li>
          <li>混合查询</li>
        </ul>
      </li>
    </ul>
  </div>
)

export default InfoView
