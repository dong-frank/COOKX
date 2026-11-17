# Design asset reproduction only; does not modify or connect to the Android App.
import bpy,bmesh,math,pathlib,json
from mathutils import Vector,Quaternion,Matrix
from bpy_extras.object_utils import world_to_camera_view
import argparse,sys
parser=argparse.ArgumentParser(description='Render the approved COOKX pantry scene from separately obtained licensed source models.')
parser.add_argument('--models-dir',type=pathlib.Path,required=True)
parser.add_argument('--output-dir',type=pathlib.Path,required=True)
parser.add_argument('--frames',type=int,nargs='+',default=list(range(20)))
args=parser.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
base=args.models_dir.resolve();out=args.output_dir.resolve()
if any(i<0 or i>19 for i in args.frames):parser.error('--frames values must be between 0 and 19')
for filename in ['kitchen-oven-and-fridge-doors.glb','kitchen-counter-and-cabinet-modules.glb']:
 if not (base/filename).is_file():parser.error('Missing licensed source model: '+filename)
(out/'frames').mkdir(parents=True,exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)
def load(slug,rootname,keep=None):
 before=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(base/(slug+'.glb')));new=set(bpy.context.scene.objects)-before
 for o in new:
  if o.animation_data:o.animation_data_clear()
 root=next(o for o in new if o.name==rootname)
 if keep:
  for o in new:
   if o.type=='MESH' and not o.name.startswith(keep):o.hide_render=True
 return root,new
fr,fo=load('kitchen-oven-and-fridge-doors','model',('fridge_body','freezer_door','fridge_door'));fr.location.x-=.17
bpy.data.objects['freezer_door'].rotation_quaternion=Quaternion((0,0,1),0);door=bpy.data.objects['fridge_door'];door.rotation_quaternion=Quaternion((0,0,1),0)
table,to=load('kitchen-counter-and-cabinet-modules','model.001',('carcass_','module_1_','module_2_','worktop_','sink_','props_tile_white_light','props_steel'))
# Keep left two cabinets, the complete sink and its original three canisters.
for o in to:
 if o.type!='MESH' or o.hide_render:continue
 world=o.matrix_world.copy();o.data.transform(world);o.parent=None;o.matrix_world=Matrix.Identity(4)
 if o.name.startswith(('carcass_','worktop_')):
  bm=bmesh.new();bm.from_mesh(o.data);res=bmesh.ops.bisect_plane(bm,geom=list(bm.verts)+list(bm.edges)+list(bm.faces),plane_co=(0,0,0),plane_no=(1,0,0),clear_inner=False,clear_outer=True,dist=.000001)
  edges=[e for e in res['geom_cut'] if isinstance(e,bmesh.types.BMEdge) and e.is_boundary]
  if edges:bmesh.ops.holes_fill(bm,edges=edges,sides=0)
  bm.normal_update();bm.to_mesh(o.data);bm.free()
 o.location.x=1.17
bpy.ops.mesh.primitive_cube_add(size=1,location=(1.161,.01,.48));end=bpy.context.object;end.name='counter_added_right_end_panel';end.dimensions=(.018,.58,.76);end.data.materials.append(bpy.data.objects['carcass_tile_white_dark'].data.materials[0]);to.add(end)
ro={o for o in to if o.name.startswith('props_tile_white_light')}
# Match original render material handling.
for mat in bpy.data.materials:
 if mat.name=='steel':
  c=(.50,.64,.53,1);mat.diffuse_color=c
  if mat.use_nodes:
   for n in mat.node_tree.nodes:
    if n.type=='BSDF_PRINCIPLED':n.inputs['Base Color'].default_value=c
bpy.ops.object.camera_add(location=(.15,-5,2.10));cam=bpy.context.object;target=Vector((.15,0,.94));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=3.05;bpy.context.scene.camera=cam
for loc,power,size in [((-3,-4,5),500,4),((3,-2,3),250,3),((0,1,4),350,3)]:
 bpy.ops.object.light_add(type='AREA',location=loc);o=bpy.context.object;o.data.energy=power;o.data.size=size;o.rotation_euler=(Vector((0,0,1))-o.location).to_track_quat('-Z','Y').to_euler()
bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.002));bpy.context.object.is_shadow_catcher=True
s=bpy.context.scene;s.render.engine='CYCLES';s.cycles.samples=20;s.cycles.use_denoising=True;s.render.resolution_x=480;s.render.resolution_y=640;s.render.resolution_percentage=100;s.render.film_transparent=True;s.world=bpy.data.worlds.new('Studio');s.world.color=(.6,.6,.6);s.view_settings.view_transform='AgX';s.render.image_settings.file_format='PNG';s.render.image_settings.color_mode='RGBA';bpy.context.view_layer.update()
def bounds(objects):
 pts=[world_to_camera_view(s,cam,o.matrix_world@Vector(c)) for o in objects if o.type=='MESH' and not o.hide_render for c in o.bound_box]
 return [min(v.x for v in pts),1-max(v.y for v in pts),max(v.x for v in pts),1-min(v.y for v in pts)]
meta={'fridgeClosed':bounds(fo),'seasoningBottles':bounds(ro),'counter':bounds(to),'counterSource':'kitchen-counter-and-cabinet-modules','retainedCabinets':['module_1','module_2'],'counterWidthMeters':1.23,'counterDepthMeters':.624,'countertopHeightMeters':.90,'geometryEdits':'Trim original x>0 to remove all counter sections right of sink. Cap cuts and add matching right end panel. Keep sink, faucet, left two cabinets and three canisters. No separate spice rack.'}
(out/'scene-layout.json').write_text(json.dumps(meta,indent=2)+'\n')
for i in args.frames:
 door.rotation_quaternion=Quaternion((0,0,1),math.radians(-100)*i/19);s.render.filepath=str(out/'frames'/f'{i:02}.png');bpy.ops.render.render(write_still=True)
