/**
 * 
 * APDPlat - Application Product Development Platform
 * Copyright (c) 2013, 杨尚川, yang-shangchuan@qq.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package org.apdplat.module.security.action;

import org.apdplat.module.module.model.Command;
import org.apdplat.module.security.model.Position;
import org.apdplat.module.security.model.User;
import org.apdplat.module.security.service.PositionService;
import org.apdplat.module.security.service.UserHolder;
import org.apdplat.platform.action.ExtJSSimpleAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Scope("prototype")
@Controller
@RequestMapping("/security")
public class PositionAction extends ExtJSSimpleAction<Position> {
    @Resource(name="positionService")
    private PositionService positionService;
    private List<Command> commands;

    @ResponseBody
    @RequestMapping("/position!store.action")
    public String store(@RequestParam(required=false) boolean recursion){
        if(recursion){
            int rootId = positionService.getRootPosition().getId();
            String json=positionService.toJson(rootId,recursion);
            return json;
        }

        return query();
    }
    @ResponseBody
    @RequestMapping("/position!query.action")
    public String query(@RequestParam(required=false) String node,
                        @RequestParam(required=false) boolean recursion,
                        @RequestParam(required=false) Integer start,
                        @RequestParam(required=false) Integer limit,
                        @RequestParam(required=false) String propertyCriteria,
                        @RequestParam(required=false) String orderCriteria,
                        @RequestParam(required=false) String queryString,
                        @RequestParam(required=false) String search){
        //如果node为null则采用普通查询方式
        if(node==null){
            super.setStart(start);
            super.setLimit(limit);
            super.setPropertyCriteria(propertyCriteria);
            super.setOrderCriteria(orderCriteria);
            super.setQueryString(queryString);
            super.setSearch("true".equals(search));
            return super.query();
        }

        //如果指定了node则采用自定义的查询方式
        if(node.trim().startsWith("root")){
            String json=positionService.toRootJson(recursion);
            return json;
        }else{
            String[] attr=node.trim().split("-");
            if(attr.length==2){
                int positionId=Integer.parseInt(attr[1]);
                String json=positionService.toJson(positionId,recursion);
                return json;
            }
        }
        return "";
    }

    /**
     * 删除岗位前，把该岗位从所有引用该岗位的用户中移除
     * @param ids
     */
    @Override
    public void prepareForDelete(Integer[] ids){
        User loginUser=UserHolder.getCurrentLoginUser();
        for(int id :ids){
            Position position=getService().retrieve(Position.class, id);
            boolean canDel=true;
            //获取拥有等待删除的角色的所有用户
            List<User> users=position.getUsers();
            for(User user : users){
                if(loginUser.getId()==user.getId()){
                    canDel=false;
                }
            }
            if(!canDel) {
                continue;
            }
            users.forEach(user -> {
                user.removePosition(position);
                getService().update(user);
            });
        }
    }
    @Override
    protected void retrieveAfterRender(Map map,Position model){
        map.put("privileges", model.getModuleCommandStr());
    }

    @Override
    public void assemblyModelForCreate(Position model) {
        model.setCommands(commands);
    }

    @Override
    public void assemblyModelForUpdate(Position model){
        //默认commands==null
        //当在修改角色的时候，如果客户端不修改commands，则commands==null
        if(commands!=null){
            model.setCommands(commands);
        }
    }
    public void setPrivileges(String privileges) {
        String[] ids=privileges.split(",");
        commands=new ArrayList<>();
        for(String id :ids){
            String[] attr=id.split("-");
            if(attr.length==2){
                if("command".equals(attr[0])){
                    Command command=getService().retrieve(Command.class, Integer.parseInt(attr[1]));
                    commands.add(command);
                }
            }
        }
    }
}